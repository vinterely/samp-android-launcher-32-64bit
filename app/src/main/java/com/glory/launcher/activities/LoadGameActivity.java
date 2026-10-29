package com.glory.launcher.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.glory.game.R;

import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.firebase.analytics.FirebaseAnalytics;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoadGameActivity extends AppCompatActivity {

    private ProgressBar downloadProgressBar;
    private TextView currentFileText;
    private TextView downloadProgressText;
    private VideoView introVideoView;
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final String CACHE_URL = "https://www.dropbox.com/scl/fi/u8y9e01rnrngvpl6bw0s4/cache.zip?rlkey=kz6qoesrulpwsr8jmegqkim9r&st=hps2dq1o&dl=1";
    private static final String CACHE_ZIP_PATH = Environment.getExternalStorageDirectory() + "/Android/data/com.glory.game/cache.zip";
    private static final String EXTRACT_DIR = Environment.getExternalStorageDirectory() + "/Android/data/com.glory.game/";
    private static final String VERSION_DIR = Environment.getExternalStorageDirectory() + "/Android/data/com.glory.game/files/";
    private static final String VERSION_FILE = VERSION_DIR + "version.txt";
    private static final String VERSION = "ver: 1.0.0";

    private static final long MEGABYTE = 1024 * 1024;
    private static final long GIGABYTE = 1024 * 1024 * 1024;
    private long totalDownloadSize = 0;
    private long downloadedBytes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenMode();

        setContentView(R.layout.launcher_load_game_activity);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        trackScreenView("LoadGameActivity");

        boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
        trackInstallStarted(isReinstall);

        initViews();
 
        playIntroVideo();
 
        new Handler().postDelayed(this::startCacheInstallation, 100);
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void trackScreenView(String screenName) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging screen view: " + e.getMessage());
        }
    }

    private void trackInstallStarted(boolean isReinstall) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("install_type", isReinstall ? "reinstall" : "first_install");
            bundle.putString("cache_url", CACHE_URL);
            bundle.putBoolean("is_reinstall", isReinstall);
            mFirebaseAnalytics.logEvent("install_started", bundle);
            Log.d("FirebaseAnalytics", "Install started, reinstall: " + isReinstall);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install start: " + e.getMessage());
        }
    }

    private void trackInstallProgress(int progress, String stage) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("progress", progress);
            bundle.putString("stage", stage);
            mFirebaseAnalytics.logEvent("install_progress", bundle);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install progress: " + e.getMessage());
        }
    }

    private void trackInstallCompleted(boolean success, String errorMessage, boolean isReinstall) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("status", success ? "success" : "failed");
            bundle.putBoolean("is_reinstall", isReinstall);
            if (!success) {
                bundle.putString("error_message", errorMessage);
            }
            bundle.putString("install_type", isReinstall ? "reinstall" : "first_install");
            mFirebaseAnalytics.logEvent("install_completed", bundle);
            Log.d("FirebaseAnalytics", "Install completed: " + (success ? "success" : "failed") + ", reinstall: " + isReinstall);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install completion: " + e.getMessage());
        }
    }

    private void initViews() {
        downloadProgressBar = findViewById(R.id.download_progress_bar);
        currentFileText = findViewById(R.id.current_file);
        downloadProgressText = findViewById(R.id.download_progress_text);
        introVideoView = findViewById(R.id.intro_video_view);

        downloadProgressBar.setProgress(0);
        downloadProgressText.setText("0.00 GB Ð¸Ð· 0.00 GB / 0%");

        boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
        if (isReinstall) {
            currentFileText.setText("ÐŸÐ•Ð Ð•Ð£Ð¡Ð¢ÐÐÐžÐ’ÐšÐ...");
        } else {
            currentFileText.setText("Ð¡ÐšÐÐ§Ð˜Ð’ÐÐ®Ð¢Ð¡Ð¯ Ð¤ÐÐ™Ð›Ð«: ");
        }
    }

    private void playIntroVideo() {
        try { 
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.intro;
            Uri videoUri = Uri.parse(videoPath);

            introVideoView.setVideoURI(videoUri);
            introVideoView.setOnPreparedListener(mp -> { 
                introVideoView.setVisibility(View.VISIBLE);
                introVideoView.start();
 
                mp.setLooping(true);
 
                introVideoView.setScaleX(1.1f);
                introVideoView.setScaleY(1.1f);
            });

            introVideoView.setOnErrorListener((mp, what, extra) -> {
                Log.e("VideoView", "Error playing video: " + what + ", " + extra);
 
                introVideoView.setVisibility(View.GONE);
                return true;
            });

        } catch (Exception e) {
            Log.e("VideoView", "Error setting up video: " + e.getMessage());
            introVideoView.setVisibility(View.GONE);
        }
    }

    private void startCacheInstallation() {
        new CacheInstallTask().execute();
    }

    private String formatFileSize(long bytes) {
        if (bytes >= GIGABYTE) {
            return String.format("%.2f GB", (double) bytes / GIGABYTE);
        } else if (bytes >= MEGABYTE) {
            return String.format("%.2f MB", (double) bytes / MEGABYTE);
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", (double) bytes / 1024);
        } else {
            return bytes + " B";
        }
    }

    private void updateProgress(int progress, long downloadedBytes, long totalBytes) {
        runOnUiThread(() -> {
            downloadProgressBar.setProgress(progress);

            String downloadedFormatted = formatFileSize(downloadedBytes);
            String totalFormatted = formatFileSize(totalBytes);
            String percentText = String.format("%s Ð¸Ð· %s / %d%%", downloadedFormatted, totalFormatted, progress);
            downloadProgressText.setText(percentText);

            String stage = "";
            boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);

            if (progress < 70) {
                if (isReinstall) {
                    currentFileText.setText("Ð¡ÐšÐÐ§Ð˜Ð’ÐÐÐ˜Ð• ÐžÐ‘ÐÐžÐ’Ð›Ð•ÐÐÐ«Ð¥ Ð¤ÐÐ™Ð›ÐžÐ’...");
                } else {
                    currentFileText.setText("Ð¡ÐšÐÐ§Ð˜Ð’ÐÐÐ˜Ð• Ð¤ÐÐ™Ð›ÐžÐ’ Ð˜Ð“Ð Ð«...");
                }
                stage = "downloading";

                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);

            } else if (progress < 100) {
                currentFileText.setText("Ð ÐÐ¡ÐŸÐÐšÐžÐ’ÐšÐ Ð¤ÐÐ™Ð›ÐžÐ’...");
                stage = "extracting";

                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);

            } else {
                if (isReinstall) {
                    currentFileText.setText("ÐŸÐ•Ð Ð•Ð£Ð¡Ð¢ÐÐÐžÐ’ÐšÐ Ð—ÐÐ’Ð•Ð Ð¨Ð•ÐÐ!");
                } else {
                    currentFileText.setText("Ð£Ð¡Ð¢ÐÐÐžÐ’ÐšÐ Ð—ÐÐ’Ð•Ð Ð¨Ð•ÐÐ!");
                }
                stage = "completed";

                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressBar.setProgress(100);
                downloadProgressText.setText(String.format("%s Ð¸Ð· %s / 100%%", totalFormatted, totalFormatted));

                // ÐžÑÑ‚Ð°Ð½Ð¾Ð²Ð¸Ñ‚ÑŒ Ð²Ð¸Ð´ÐµÐ¾ Ð¿Ñ€Ð¸ Ð·Ð°Ð²ÐµÑ€ÑˆÐµÐ½Ð¸Ð¸ ÑƒÑÑ‚Ð°Ð½Ð¾Ð²ÐºÐ¸
                if (introVideoView != null && introVideoView.isPlaying()) {
                    introVideoView.stopPlayback();
                    introVideoView.setVisibility(View.GONE);
                }
            }

            if (progress % 10 == 0) {
                trackInstallProgress(progress, stage);
            }
        });
    }

    private class CacheInstallTask extends AsyncTask<Void, Object, Boolean> {
        private String errorMessage = "";
        private long totalBytes = 0;
        private long downloadedBytes = 0;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                publishProgress(0, 0L, 0L);

                File extractDir = new File(EXTRACT_DIR);
                if (!extractDir.exists()) {
                    extractDir.mkdirs();
                }

                if (!downloadCacheZip()) {
                    errorMessage = "Download failed";
                    return false;
                }

                publishProgress(70, totalBytes, totalBytes);
                if (!extractZip(CACHE_ZIP_PATH, EXTRACT_DIR)) {
                    errorMessage = "Extraction failed";
                    return false;
                }

                publishProgress(95, totalBytes, totalBytes);
                new File(CACHE_ZIP_PATH).delete();

                publishProgress(98, totalBytes, totalBytes);
                if (!createVersionFile()) {
                    errorMessage = "Version file creation failed";
                    return false;
                }

                publishProgress(100, totalBytes, totalBytes);
                return true;

            } catch (Exception e) {
                errorMessage = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        private boolean downloadCacheZip() {
            try {
                URL url = new URL(CACHE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP error: " + connection.getResponseCode();
                    return false;
                }

                int fileLength = connection.getContentLength();
                totalDownloadSize = fileLength;
                totalBytes = totalDownloadSize;

                downloadedBytes = 0;
                publishProgress(0, 0L, totalBytes);

                InputStream input = connection.getInputStream();
                OutputStream output = new FileOutputStream(CACHE_ZIP_PATH);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    downloadedBytes = total;

                    if (fileLength > 0) {
                        int progress = (int) (total * 70 / fileLength);
                        publishProgress(progress, downloadedBytes, totalBytes);
                    } else {
                        int progress = (int) (downloadedBytes * 70 / Math.max(1, totalBytes));
                        publishProgress(progress, downloadedBytes, totalBytes);
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                connection.disconnect();
                return true;

            } catch (Exception e) {
                errorMessage = "Download exception: " + e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        private boolean extractZip(String zipFile, String extractTo) {
            try {
                FileInputStream fis = new FileInputStream(zipFile);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                ZipEntry entry;

                byte[] buffer = new byte[1024];
                int count;
                int totalEntries = 0;
                int processedEntries = 0;

                ZipInputStream counterZis = new ZipInputStream(new FileInputStream(zipFile));
                while (counterZis.getNextEntry() != null) {
                    totalEntries++;
                }
                counterZis.close();

                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    File entryFile = new File(extractTo + entryName);

                    if (entry.isDirectory()) {
                        entryFile.mkdirs();
                    } else {
                        File parentDir = new File(entryFile.getParent());
                        if (!parentDir.exists()) {
                            parentDir.mkdirs();
                        }

                        FileOutputStream fos = new FileOutputStream(entryFile);
                        BufferedOutputStream dest = new BufferedOutputStream(fos, buffer.length);

                        while ((count = zis.read(buffer, 0, buffer.length)) != -1) {
                            dest.write(buffer, 0, count);
                        }

                        dest.flush();
                        dest.close();
                    }

                    processedEntries++;
                    int progress = 70 + (processedEntries * 25 / totalEntries);
                    publishProgress(progress, totalBytes, totalBytes);

                    zis.closeEntry();
                }

                zis.close();
                fis.close();
                return true;

            } catch (Exception e) {
                errorMessage = "Extraction exception: " + e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        private boolean createVersionFile() {
            try {
                File versionFile = new File(VERSION_FILE);
                FileWriter writer = new FileWriter(versionFile);
                writer.write(VERSION);
                writer.flush();
                writer.close();
                return true;
            } catch (Exception e) {
                errorMessage = "Version file exception: " + e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (values.length >= 3 && values[0] instanceof Integer) {
                int progress = (Integer) values[0];
                long downloaded = values[1] instanceof Long ? (Long) values[1] :
                        values[1] instanceof Integer ? ((Integer) values[1]).longValue() : 0;
                long total = values[2] instanceof Long ? (Long) values[2] :
                        values[2] instanceof Integer ? ((Integer) values[2]).longValue() : 0;

                updateProgress(progress, downloaded, total);
            } else if (values.length == 1 && values[0] instanceof Integer) {
                updateProgress((Integer) values[0], downloadedBytes, totalBytes);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);

            if (success) {
                String totalFormatted = formatFileSize(totalBytes);
                downloadProgressText.setText(String.format("%s Ð¸Ð· %s / 100%%", totalFormatted, totalFormatted));
                trackInstallCompleted(true, "", isReinstall);
                new Handler().postDelayed(() -> {
                    startActivity(new Intent(LoadGameActivity.this, HomeActivity.class));
                    finish();
                }, 1500);
            } else {
                if (isReinstall) {
                    currentFileText.setText("ÐžÐ¨Ð˜Ð‘ÐšÐ ÐŸÐ•Ð Ð•Ð£Ð¡Ð¢ÐÐÐžÐ’ÐšÐ˜");
                } else {
                    currentFileText.setText("ÐžÐ¨Ð˜Ð‘ÐšÐ Ð£Ð¡Ð¢ÐÐÐžÐ’ÐšÐ˜");
                }
                downloadProgressText.setText("ÐžÐ¨Ð˜Ð‘ÐšÐ");
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);

                // ÐžÑÑ‚Ð°Ð½Ð¾Ð²Ð¸Ñ‚ÑŒ Ð²Ð¸Ð´ÐµÐ¾ Ð¿Ñ€Ð¸ Ð¾ÑˆÐ¸Ð±ÐºÐµ
                if (introVideoView != null && introVideoView.isPlaying()) {
                    introVideoView.stopPlayback();
                    introVideoView.setVisibility(View.GONE);
                }

                trackInstallCompleted(false, errorMessage, isReinstall);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause(); 
        if (introVideoView != null && introVideoView.isPlaying()) {
            introVideoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume(); 
        if (introVideoView != null && !introVideoView.isPlaying()) {
            introVideoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy(); 
        if (introVideoView != null) {
            introVideoView.stopPlayback();
        }
    }
}
