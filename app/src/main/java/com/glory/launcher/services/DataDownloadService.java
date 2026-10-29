package com.glory.launcher.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.glory.Config;
import com.glory.game.R;
import com.glory.launcher.activities.DataDownloadActivity;
import com.glory.launcher.activities.MainActivity;
import com.glory.launcher.network.ManifestManager;
import com.glory.launcher.network.model.LauncherManifest;
import com.glory.launcher.util.PersianUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DataDownloadService extends Service {

    private static final String TAG = "DataDownloadService";
    private static final String TAG_EXTRACT = "DATA_EXTRACT";
    private static final String CHANNEL_ID = "glory_data_sync_channel";
    private static final int NOTIFICATION_ID = 2026;

    public static final String ACTION_START = "com.glory.action.START_DOWNLOAD";
    public static final String ACTION_PAUSE = "com.glory.action.PAUSE_DOWNLOAD";
    public static final String ACTION_RESUME = "com.glory.action.RESUME_DOWNLOAD";
    public static final String ACTION_CANCEL = "com.glory.action.CANCEL_DOWNLOAD";

    public enum DownloadStep {
        IDLE,
        CHECKING_SPACE,
        DOWNLOADING,
        VERIFYING,
        EXTRACTING,
        COMPLETED,
        PAUSED,
        ERROR
    }

    public static class ProgressInfo {
        public DownloadStep step = DownloadStep.IDLE;
        public long bytesDownloaded = 0;
        public long totalBytes = 0;
        public int percent = 0;
        public double speedBytesPerSec = 0;
        public long etaSeconds = 0;
        public String statusText = "";
        public String errorMessage = "";
    }

    public interface DownloadListener {
        void onProgress(ProgressInfo info);
        void onComplete();
        void onError(String message);
    }

    private static final Object lock = new Object();
    private static final List<DownloadListener> listeners = new ArrayList<>();
    private static final ProgressInfo currentProgress = new ProgressInfo();
    private static LauncherManifest activeManifest;
    private static int extractionFailures = 0;

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean isPaused = false;
    private volatile boolean isCancelled = false;
    private NotificationManager notificationManager;

    public static void addListener(DownloadListener listener) {
        synchronized (lock) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
                listener.onProgress(currentProgress);
            }
        }
    }

    public static void removeListener(DownloadListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    public static ProgressInfo getCurrentProgress() {
        synchronized (lock) {
            return currentProgress;
        }
    }

    public static void setManifest(LauncherManifest manifest) {
        synchronized (lock) {
            activeManifest = manifest;
        }
    }

    public static void start(Context context, LauncherManifest manifest) {
        setManifest(manifest);
        Intent intent = new Intent(context, DataDownloadService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void pause(Context context) {
        Intent intent = new Intent(context, DataDownloadService.class);
        intent.setAction(ACTION_PAUSE);
        context.startService(intent);
    }

    public static void resume(Context context) {
        Intent intent = new Intent(context, DataDownloadService.class);
        intent.setAction(ACTION_RESUME);
        context.startService(intent);
    }

    public static void cancel(Context context) {
        Intent intent = new Intent(context, DataDownloadService.class);
        intent.setAction(ACTION_CANCEL);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startForegroundNotification("در حال آماده‌سازی دانلود...", 0);
            isPaused = false;
            isCancelled = false;
            downloadExecutor.execute(this::runDownloadPipeline);
        } else if (ACTION_PAUSE.equals(action)) {
            isPaused = true;
            updateProgress(DownloadStep.PAUSED, currentProgress.bytesDownloaded, currentProgress.totalBytes,
                    currentProgress.percent, 0, 0, "دانلود متوقف شد");
        } else if (ACTION_RESUME.equals(action)) {
            if (isPaused) {
                isPaused = false;
                downloadExecutor.execute(this::runDownloadPipeline);
            }
        } else if (ACTION_CANCEL.equals(action)) {
            isCancelled = true;
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void runDownloadPipeline() {
        LauncherManifest manifest;
        synchronized (lock) {
            manifest = activeManifest;
            if (manifest == null) {
                manifest = ManifestManager.getInstance().getManifest(this);
                activeManifest = manifest;
            }
        }

        if (manifest == null || manifest.getData() == null) {
            dispatchError("اطلاعات دیتای بازی در دسترس نیست");
            return;
        }

        LauncherManifest.DataInfo dataInfo = manifest.getData();
        List<LauncherManifest.DataFile> files = dataInfo.getFiles();
        if (files == null || files.isEmpty()) {
            dispatchError("فایل دانلودی در مانیفست تعریف نشده است");
            return;
        }

        String gamePath = Config.GAME_PATH;
        if (gamePath == null || gamePath.isEmpty()) {
            gamePath = "/storage/emulated/0/GloryMobile/";
            Config.setGamePath(gamePath);
        }
        File gameDir = new File(gamePath);
        if (!gameDir.exists()) {
            gameDir.mkdirs();
        }

        File tmpDir = new File(gameDir, ".glory_tmp");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        // If extraction failure happened on attempt 1, and zip exists, jump straight to extraction
        if (extractionFailures == 1) {
            boolean allZipsPresent = true;
            for (LauncherManifest.DataFile df : files) {
                File zipFile = new File(tmpDir, df.getName());
                if (!zipFile.exists() || zipFile.length() == 0) {
                    allZipsPresent = false;
                    break;
                }
            }
            if (allZipsPresent) {
                Log.i(TAG_EXTRACT, "Retry attempt 1: Re-extracting existing zip files without re-downloading.");
                performExtraction(gameDir, tmpDir, files, dataInfo);
                return;
            }
        } else if (extractionFailures >= 2) {
            // Delete corrupt zip and re-download from scratch once
            Log.w(TAG_EXTRACT, "Retry attempt 2: Extraction failed twice, deleting cached zip and re-downloading.");
            for (LauncherManifest.DataFile df : files) {
                File zipFile = new File(tmpDir, df.getName());
                if (zipFile.exists()) zipFile.delete();
                File partFile = new File(tmpDir, df.getName() + ".part");
                if (partFile.exists()) partFile.delete();
            }
            extractionFailures = 0;
        }

        updateProgress(DownloadStep.CHECKING_SPACE, 0, 0, 0, 0, 0, "بررسی فضای ذخیره‌سازی...");

        String baseUrl = dataInfo.getBaseUrl();
        if (baseUrl == null) baseUrl = "";
        if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        long overallTotalBytes = 0;
        long overallBytesDownloaded = 0;

        // Pre-query file headers if size == 0 to determine total size and space needed
        long totalZipBytes = 0;
        for (LauncherManifest.DataFile df : files) {
            long sz = df.getSize();
            if (sz <= 0) {
                sz = queryFileSize(baseUrl + df.getName());
                if (sz > 0) df.setSize(sz);
            }
            totalZipBytes += sz;
        }

        // Free space needed = zip Content-Length + manifest.data.extractedSize + 100 MB
        long neededBytes = totalZipBytes + dataInfo.getExtractedSize() + (100 * 1024 * 1024L);
        try {
            StatFs statFs = new StatFs(gameDir.getAbsolutePath());
            long availableBytes = statFs.getAvailableBytes();
            if (totalZipBytes > 0 && availableBytes < neededBytes) {
                dispatchError("فضای کافی در حافظه دستگاه موجود نیست. حداقل " + PersianUtils.formatBytes(neededBytes) + " فضا نیاز است.");
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check free space: " + e.getMessage());
        }

        overallTotalBytes = totalZipBytes;

        for (LauncherManifest.DataFile df : files) {
            File completeFile = new File(tmpDir, df.getName());
            File partFile = new File(tmpDir, df.getName() + ".part");
            if (completeFile.exists()) {
                overallBytesDownloaded += completeFile.length();
            } else if (partFile.exists()) {
                overallBytesDownloaded += partFile.length();
            }
        }

        for (int i = 0; i < files.size(); i++) {
            if (isCancelled) return;
            if (isPaused) return;

            LauncherManifest.DataFile df = files.get(i);
            File completeFile = new File(tmpDir, df.getName());
            File partFile = new File(tmpDir, df.getName() + ".part");

            if (!completeFile.exists() || (df.getSize() > 0 && completeFile.length() != df.getSize())) {
                boolean downloadSuccess = false;
                int retries = 0;

                while (!downloadSuccess && retries < 3) {
                    if (isCancelled || isPaused) return;

                    long existingBytes = partFile.exists() ? partFile.length() : 0;
                    if (df.getSize() > 0 && existingBytes > df.getSize()) {
                        partFile.delete();
                        existingBytes = 0;
                    }

                    HttpURLConnection conn = null;
                    try {
                        String fileUrl = baseUrl + df.getName();
                        URL url = new URL(fileUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(15000);
                        conn.setRequestProperty("User-Agent", ManifestManager.getUserAgent());

                        if (existingBytes > 0) {
                            conn.setRequestProperty("Range", "bytes=" + existingBytes + "-");
                        }

                        int code = conn.getResponseCode();
                        boolean append = false;
                        long fileTargetLength = df.getSize();

                        if (code == HttpURLConnection.HTTP_PARTIAL) {
                            append = true;
                            String contentRange = conn.getHeaderField("Content-Range");
                            if (contentRange != null && contentRange.contains("/")) {
                                try {
                                    long totalFromRange = Long.parseLong(contentRange.substring(contentRange.lastIndexOf('/') + 1).trim());
                                    if (totalFromRange > 0) fileTargetLength = totalFromRange;
                                } catch (Exception ignored) {}
                            }
                        } else if (code == HttpURLConnection.HTTP_OK) {
                            append = false;
                            existingBytes = 0;
                            long cl = conn.getContentLength();
                            if (cl > 0) fileTargetLength = cl;
                        } else {
                            throw new IllegalStateException("HTTP error " + code + " on " + fileUrl);
                        }

                        if (df.getSize() <= 0 && fileTargetLength > 0) {
                            df.setSize(fileTargetLength);
                            overallTotalBytes = fileTargetLength;
                        }

                        try (InputStream in = new BufferedInputStream(conn.getInputStream());
                             OutputStream out = new FileOutputStream(partFile, append)) {

                            byte[] buffer = new byte[32 * 1024];
                            int read;
                            long lastUpdate = System.currentTimeMillis();
                            long bytesSinceLastUpdate = 0;

                            while ((read = in.read(buffer)) != -1) {
                                if (isCancelled) return;
                                if (isPaused) return;

                                out.write(buffer, 0, read);
                                existingBytes += read;
                                overallBytesDownloaded += read;
                                bytesSinceLastUpdate += read;

                                long now = System.currentTimeMillis();
                                if (now - lastUpdate >= 250) {
                                    double speed = (bytesSinceLastUpdate * 1000.0) / (now - lastUpdate);
                                    long remainingBytes = Math.max(0, overallTotalBytes - overallBytesDownloaded);
                                    long eta = speed > 0 ? (long) (remainingBytes / speed) : 0;
                                    int percent = overallTotalBytes > 0 ? (int) ((overallBytesDownloaded * 100) / overallTotalBytes) : 0;
                                    if (percent > 100) percent = 100;

                                    updateProgress(DownloadStep.DOWNLOADING, overallBytesDownloaded, overallTotalBytes,
                                            percent, speed, eta, "در حال دانلود دیتای بازی...");
                                    lastUpdate = now;
                                    bytesSinceLastUpdate = 0;
                                }
                            }
                            out.flush();
                        }

                        // Verify size only if size > 0
                        if (df.getSize() > 0 && partFile.length() != df.getSize()) {
                            retries++;
                            Log.w(TAG, "Size mismatch for " + df.getName() + ": expected " + df.getSize() + ", got " + partFile.length());
                            if (retries >= 3) {
                                dispatchError("خطا در حجم فایل دانلود شده (" + df.getName() + ")");
                                return;
                            }
                            Thread.sleep(1000 * retries);
                            continue;
                        }

                        downloadSuccess = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Download error on " + df.getName(), e);
                        retries++;
                        if (retries >= 3) {
                            dispatchError("خطا در اتصال به سرور: " + e.getMessage());
                            return;
                        }
                        try {
                            Thread.sleep(1000 * retries);
                        } catch (InterruptedException ignored) {}
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                }

                // Verify SHA-256 only if non-empty
                if (df.getSha256() != null && !df.getSha256().trim().isEmpty()) {
                    updateProgress(DownloadStep.VERIFYING, overallBytesDownloaded, overallTotalBytes,
                            100, 0, 0, "در حال بررسی سلامت فایل...");
                    if (!verifySha256(partFile, df.getSha256())) {
                        partFile.delete();
                        dispatchError("کد اعتبارسنجی فایل (SHA256) مطابقت ندارد");
                        return;
                    }
                }

                // Rename .part to final zip
                if (completeFile.exists()) completeFile.delete();
                partFile.renameTo(completeFile);
            }
        }

        performExtraction(gameDir, tmpDir, files, dataInfo);
    }

    private void performExtraction(File gameDir, File tmpDir, List<LauncherManifest.DataFile> files, LauncherManifest.DataInfo dataInfo) {
        updateProgress(DownloadStep.EXTRACTING, currentProgress.totalBytes, currentProgress.totalBytes,
                100, 0, 0, "در حال استخراج و نصب فایل‌ها...");

        for (LauncherManifest.DataFile df : files) {
            if (isCancelled || isPaused) return;

            File zipFile = new File(tmpDir, df.getName());
            if (!zipFile.exists()) {
                dispatchError("فایل فشرده دیتا یافت نشد: " + df.getName());
                return;
            }

            try {
                extractZipCompletely(zipFile, gameDir, dataInfo.getExtractedSize());
            } catch (Exception e) {
                extractionFailures++;
                Log.e(TAG_EXTRACT, "Extraction failed (attempt " + extractionFailures + "): " + e.getMessage(), e);
                dispatchError("خطا در استخراج فایل‌های بازی: " + e.getMessage());
                return;
            }
        }

        // Only after ALL entries are extracted and verified: delete the zip(s) and .glory_tmp
        try {
            for (LauncherManifest.DataFile df : files) {
                File f = new File(tmpDir, df.getName());
                if (f.exists()) f.delete();
                File part = new File(tmpDir, df.getName() + ".part");
                if (part.exists()) part.delete();
            }
            tmpDir.delete();
        } catch (Exception ignored) {}

        extractionFailures = 0;

        // Run evaluateGameDataStatus() (expect COMPLETE; else show error with retry)
        MainActivity.GameDataStatus finalStatus = MainActivity.evaluateGameDataStatus(this);
        if (finalStatus != MainActivity.GameDataStatus.COMPLETE) {
            dispatchError("برخی از پوشه‌های اصلی بازی پس از استخراج یافت نشدند (وضعیت: " + finalStatus + ")");
            return;
        }

        updateProgress(DownloadStep.COMPLETED, currentProgress.totalBytes, currentProgress.totalBytes,
                100, 0, 0, "نصب دیتای بازی با موفقیت پایان یافت");

        showCompletionNotification();

        mainHandler.post(() -> {
            synchronized (lock) {
                for (DownloadListener l : new ArrayList<>(listeners)) {
                    l.onComplete();
                }
            }
        });
    }

    private void extractZipCompletely(File zipFile, File targetDir, long expectedExtractedSize) throws Exception {
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFile);
            int totalZipEntries = zf.size();
            int totalZipFileEntries = 0;
            long totalUncompressedBytesInZip = 0;

            // Pre-scan: count file entries, sum uncompressed bytes, and detect wrapper folder rule
            Set<String> topLevelNames = new HashSet<>();
            Enumeration<? extends ZipEntry> enScan = zf.entries();
            while (enScan.hasMoreElements()) {
                ZipEntry entry = enScan.nextElement();
                if (!entry.isDirectory()) {
                    totalZipFileEntries++;
                    long sz = entry.getSize();
                    if (sz > 0) totalUncompressedBytesInZip += sz;
                }
                String name = entry.getName().replace('\\', '/');
                while (name.startsWith("/")) name = name.substring(1);
                if (!name.isEmpty()) {
                    int slash = name.indexOf('/');
                    String top = (slash != -1) ? name.substring(0, slash) : name;
                    topLevelNames.add(top);
                }
            }

            // Wrapper-folder rule: only if EVERY entry shares one single top-level folder
            // AND none of the top-level names is anim, data, models, texdb, SAMP, CINFO.BIN or stream.ini, strip that wrapper folder.
            String wrapperPrefix = "";
            if (topLevelNames.size() == 1) {
                String singleTop = topLevelNames.iterator().next();
                String singleTopLower = singleTop.toLowerCase(Locale.US);
                Set<String> standardNames = new HashSet<>();
                standardNames.add("anim");
                standardNames.add("data");
                standardNames.add("models");
                standardNames.add("texdb");
                standardNames.add("samp");
                standardNames.add("cinfo.bin");
                standardNames.add("stream.ini");

                if (!standardNames.contains(singleTopLower)) {
                    wrapperPrefix = singleTop + "/";
                    Log.i(TAG_EXTRACT, "Wrapper folder detected and stripped: " + singleTop);
                } else {
                    Log.i(TAG_EXTRACT, "No wrapper folder stripped (single top-level is standard: " + singleTop + ")");
                }
            } else {
                Log.i(TAG_EXTRACT, "No wrapper folder stripped (multiple top-level items: " + topLevelNames.size() + ")");
            }

            String targetCanonical = targetDir.getCanonicalPath();
            int extractedFilesCount = 0;
            long totalBytesExtracted = 0;
            boolean foundSettingsIni = false;
            String settingsIniPath = "";
            long settingsIniSize = 0;

            long totalTargetBytes = totalUncompressedBytesInZip > 0 ? totalUncompressedBytesInZip : expectedExtractedSize;

            Enumeration<? extends ZipEntry> enExtract = zf.entries();
            byte[] buffer = new byte[32 * 1024];

            while (enExtract.hasMoreElements()) {
                if (isCancelled || isPaused) return;

                ZipEntry entry = enExtract.nextElement();
                String rawName = entry.getName().replace('\\', '/');
                while (rawName.startsWith("/")) rawName = rawName.substring(1);

                if (wrapperPrefix.length() > 0) {
                    if (rawName.equals(wrapperPrefix) || rawName.equals(wrapperPrefix.substring(0, wrapperPrefix.length() - 1))) {
                        // Skip the wrapper folder entry itself
                        continue;
                    }
                    if (rawName.startsWith(wrapperPrefix)) {
                        rawName = rawName.substring(wrapperPrefix.length());
                    }
                }

                if (rawName.isEmpty()) continue;

                File targetFile = new File(targetDir, rawName);
                String destCanonical = targetFile.getCanonicalPath();

                // Zip-slip protection: reject any entry whose resolved path leaves the target directory
                if (!destCanonical.startsWith(targetCanonical + File.separator) && !destCanonical.equals(targetCanonical)) {
                    throw new SecurityException("Zip slip detected in entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    // Write each file to a temporary name in the same folder (.glory_new)
                    File tmpNewFile = new File(parent, targetFile.getName() + ".glory_new");
                    if (tmpNewFile.exists()) tmpNewFile.delete();

                    CRC32 crc32 = new CRC32();
                    long bytesWritten = 0;

                    try (InputStream is = zf.getInputStream(entry);
                         OutputStream os = new FileOutputStream(tmpNewFile)) {

                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            if (isCancelled || isPaused) {
                                tmpNewFile.delete();
                                return;
                            }
                            os.write(buffer, 0, read);
                            crc32.update(buffer, 0, read);
                            bytesWritten += read;
                            totalBytesExtracted += read;

                            if (totalTargetBytes > 0) {
                                int percent = (int) ((totalBytesExtracted * 100) / totalTargetBytes);
                                if (percent > 100) percent = 100;
                                updateProgress(DownloadStep.EXTRACTING, totalBytesExtracted, totalTargetBytes,
                                        percent, 0, 0, "در حال استخراج: " + rawName);
                            }
                        }
                        os.flush();
                    }

                    // Integrity check: compare size
                    long expectedSize = entry.getSize();
                    if (expectedSize >= 0 && bytesWritten != expectedSize) {
                        tmpNewFile.delete();
                        throw new IOException("Size mismatch for " + entry.getName() + ": expected " + expectedSize + ", wrote " + bytesWritten);
                    }

                    // Integrity check: compare CRC32
                    long expectedCrc = entry.getCrc();
                    if (expectedCrc != -1 && crc32.getValue() != expectedCrc) {
                        tmpNewFile.delete();
                        throw new IOException("CRC32 mismatch for " + entry.getName() + ": expected " + expectedCrc + ", got " + crc32.getValue());
                    }

                    // Overwrite final destination file
                    if (targetFile.exists()) {
                        targetFile.delete();
                    }
                    if (!tmpNewFile.renameTo(targetFile)) {
                        // Fallback copy + delete if atomic rename fails
                        try (InputStream in = new FileInputStream(tmpNewFile);
                             OutputStream out = new FileOutputStream(targetFile)) {
                            int r;
                            while ((r = in.read(buffer)) != -1) {
                                out.write(buffer, 0, r);
                            }
                            out.flush();
                        }
                        tmpNewFile.delete();
                    }

                    extractedFilesCount++;

                    // Check settings.ini
                    if (rawName.equalsIgnoreCase("SAMP/settings.ini") || rawName.equalsIgnoreCase("SAMP\\settings.ini")) {
                        foundSettingsIni = true;
                        settingsIniPath = targetFile.getAbsolutePath();
                        settingsIniSize = bytesWritten;
                    }
                }
            }

            // Assert that the number of extracted file entries equals the number of file entries in the zip
            if (extractedFilesCount != totalZipFileEntries) {
                throw new IOException("Extracted file count mismatch: expected " + totalZipFileEntries + ", extracted " + extractedFilesCount);
            }

            // Log with tag DATA_EXTRACT
            Log.i(TAG_EXTRACT, "Extraction completed successfully: total entries=" + totalZipEntries +
                    ", files extracted=" + extractedFilesCount + ", total bytes=" + totalBytesExtracted);

            if (foundSettingsIni) {
                Log.i(TAG_EXTRACT, "settings.ini extracted: " + settingsIniPath + " " + settingsIniSize);
            } else {
                Log.w(TAG_EXTRACT, "WARNING: Zip has no SAMP/settings.ini!");
            }

        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private long queryFileSize(String fileUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", ManifestManager.getUserAgent());
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                return conn.getContentLength();
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return 0;
    }

    private boolean verifySha256(File file, String expectedSha256) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = is.read(buf)) != -1) {
                digest.update(buf, 0, r);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format(java.util.Locale.US, "%02x", b));
            }
            return sb.toString().equalsIgnoreCase(expectedSha256.trim());
        } catch (Exception e) {
            Log.e(TAG, "SHA256 check failed", e);
            return false;
        }
    }

    private void updateProgress(DownloadStep step, long downloaded, long total, int percent,
                                double speed, long eta, String text) {
        synchronized (lock) {
            currentProgress.step = step;
            currentProgress.bytesDownloaded = downloaded;
            currentProgress.totalBytes = total;
            currentProgress.percent = percent;
            currentProgress.speedBytesPerSec = speed;
            currentProgress.etaSeconds = eta;
            currentProgress.statusText = text;
        }

        updateNotification(text, percent);

        mainHandler.post(() -> {
            synchronized (lock) {
                for (DownloadListener l : new ArrayList<>(listeners)) {
                    l.onProgress(currentProgress);
                }
            }
        });
    }

    private void dispatchError(String errorMessage) {
        synchronized (lock) {
            currentProgress.step = DownloadStep.ERROR;
            currentProgress.errorMessage = errorMessage;
            currentProgress.statusText = errorMessage;
        }

        updateNotification("خطا: " + errorMessage, 0);

        mainHandler.post(() -> {
            synchronized (lock) {
                for (DownloadListener l : new ArrayList<>(listeners)) {
                    l.onError(errorMessage);
                }
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "دانلود دیتای بازی",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("اعلان پیشرفت دانلود و نصب دیتای بازی");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundNotification(String text, int progress) {
        Notification notification = buildNotification(text, progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String text, int progress) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(text, progress));
        }
    }

    private void showCompletionNotification() {
        if (notificationManager != null) {
            Intent intent = new Intent(this, DataDownloadActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("دیتای بازی GloryRP")
                    .setContentText("نصب دیتای بازی با موفقیت به پایان رسید.")
                    .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .build();

            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String text, int progress) {
        Intent intent = new Intent(this, DataDownloadActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("دانلود دیتای بازی GloryRP")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        downloadExecutor.shutdownNow();
    }
}
