package com.glory.launcher.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.glory.game.BuildConfig;
import com.glory.launcher.network.model.LauncherManifest;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManifestManager {

    private static final String TAG = "ManifestManager";
    private static final String CACHE_FILE_NAME = "manifest_cache.json";
    private static final int TIMEOUT_MS = 8000;

    private static volatile ManifestManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private LauncherManifest cachedManifest;

    public interface ManifestCallback {
        void onSuccess(LauncherManifest manifest);
        void onError(Exception exception, LauncherManifest fallbackManifest);
    }

    private ManifestManager() {
    }

    public static ManifestManager getInstance() {
        if (instance == null) {
            synchronized (ManifestManager.class) {
                if (instance == null) {
                    instance = new ManifestManager();
                }
            }
        }
        return instance;
    }

    public static String getUserAgent() {
        return "GloryLauncher/" + BuildConfig.VERSION_NAME;
    }

    public synchronized LauncherManifest getCachedManifest() {
        return cachedManifest;
    }

    public synchronized LauncherManifest getManifest(Context context) {
        if (cachedManifest != null) {
            return cachedManifest;
        }
        cachedManifest = loadFromCacheFile(context);
        return cachedManifest;
    }

    public void fetchManifest(Context context, ManifestCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(Links.MANIFEST_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", getUserAgent());
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream is = conn.getInputStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                    }
                    String json = sb.toString().trim();
                    LauncherManifest manifest = gson.fromJson(json, LauncherManifest.class);

                    if (manifest != null && manifest.getSchema() >= 1) {
                        synchronized (ManifestManager.this) {
                            cachedManifest = manifest;
                        }
                        saveToCacheFile(context, json);
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess(manifest));
                        }
                        return;
                    } else {
                        throw new IllegalStateException("Invalid manifest format or schema");
                    }
                } else {
                    throw new IllegalStateException("HTTP error response: " + responseCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Manifest fetch failed, attempting cache fallback: " + e.getMessage());
                LauncherManifest fallback = loadFromCacheFile(context);
                synchronized (ManifestManager.this) {
                    if (fallback != null) {
                        cachedManifest = fallback;
                    }
                }
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e, fallback));
                }
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private synchronized void saveToCacheFile(Context context, String json) {
        if (context == null || json == null || json.isEmpty()) return;
        try {
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving manifest to cache", e);
        }
    }

    private synchronized LauncherManifest loadFromCacheFile(Context context) {
        if (context == null) return null;
        try {
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
            if (!cacheFile.exists() || cacheFile.length() == 0) return null;

            StringBuilder sb = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return gson.fromJson(sb.toString(), LauncherManifest.class);
        } catch (Exception e) {
            Log.e(TAG, "Error loading manifest from cache", e);
            return null;
        }
    }
}
