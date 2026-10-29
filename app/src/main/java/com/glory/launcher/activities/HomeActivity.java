package com.glory.launcher.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;
import androidx.core.content.FileProvider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.glory.game.R;
import com.glory.game.SAMP;
import com.glory.game.ui.LoadingScreen;
import com.glory.launcher.api.API;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAnalytics mFirebaseAnalytics;

    private EditText nicknameEditText;
    private ConstraintLayout donateButton, logButton, playButton;
    private ImageView telegramButton, vkButton, webButton;

    private Wini mWini = null;
    private LoadingScreen loadingScreen;

    private boolean isHudShown = false;
    private boolean isGameLaunched = false;
    private boolean isLoadingScreenShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate started");

        try {
            setTheme(R.style.Theme_AppCompat_Light_NoActionBar_FullScreen);

            setFullScreenMode();

            setContentView(R.layout.launcher_home_activity);

            Log.d(TAG, "ContentView set");

            initFirebaseAnalytics();

            initLoadingScreen();

            initViews();

            setupClickListeners();

            loadNickname();

            if (isFromGameReturn()) {
                checkHudStatus();
            }

            checkCacheOnStart();

            Log.d(TAG, "onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Critical error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð¿ÑƒÑÐºÐ° Ð¿Ñ€Ð¸Ð»Ð¾Ð¶ÐµÐ½Ð¸Ñ", Toast.LENGTH_LONG).show();
            finishAffinity();
        }
    }

    private boolean isFromGameReturn() {
        SharedPreferences prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
        boolean fromGame = prefs.getBoolean("from_game_return", false);
        if (fromGame) {
            prefs.edit().remove("from_game_return").apply();
        }
        return fromGame;
    }

    private void initFirebaseAnalytics() {
        try {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            if (mFirebaseAnalytics != null) {
                trackAppOpen();
            } else {
                Log.w(TAG, "FirebaseAnalytics is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting FirebaseAnalytics: " + e.getMessage());
            mFirebaseAnalytics = null;
        }
    }

    private void initLoadingScreen() {
        try {
            loadingScreen = new LoadingScreen(this);
        } catch (Exception e) {
            Log.e(TAG, "Error creating LoadingScreen: " + e.getMessage());
            loadingScreen = null;
        }
    }

    private void setFullScreenMode() {
        try {
            Window window = getWindow();

            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);

                window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

                window.getInsetsController().hide(
                        android.view.WindowInsets.Type.statusBars() |
                                android.view.WindowInsets.Type.navigationBars()
                );

                window.getInsetsController().setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

            } else {
                View decorView = window.getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                decorView.setSystemUiVisibility(uiOptions);

                window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    window.setAttributes(params);
                }
            }

            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        } catch (Exception e) {
            Log.e(TAG, "Error in setFullScreenMode: " + e.getMessage());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    getWindow().getInsetsController().hide(
                            android.view.WindowInsets.Type.statusBars() |
                                    android.view.WindowInsets.Type.navigationBars()
                    );
                } else {
                    View decorView = getWindow().getDecorView();
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                    decorView.setSystemUiVisibility(uiOptions);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onWindowFocusChanged: " + e.getMessage());
            }
        }
    }

    private void checkCacheOnStart() {
        try {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isCacheInstalled()) {
                        startCacheInstallation(false);
                    }
                }
            }, 1500);
        } catch (Exception e) {
            Log.e(TAG, "Error in checkCacheOnStart: " + e.getMessage());
        }
    }

    private void startCacheInstallation(boolean isReinstall) {
        try {
            trackCacheInstallStarted();
            Intent installIntent = new Intent(HomeActivity.this, LoadGameActivity.class);
            installIntent.putExtra("is_reinstall", isReinstall);
            startActivity(installIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error in startCacheInstallation: " + e.getMessage());
            Toast.makeText(this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð¿ÑƒÑÐºÐ° ÑƒÑÑ‚Ð°Ð½Ð¾Ð²ÐºÐ¸ ÐºÑÑˆÐ°", Toast.LENGTH_SHORT).show();
        }
    }

    public void onHudShown() {
        Log.d(TAG, "HUD shown callback received");
        isHudShown = true;

        try {
            if (loadingScreen != null && loadingScreen.isVisible()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            loadingScreen.hide();
                        } catch (Exception e) {
                            Log.e(TAG, "Error hiding loading screen: " + e.getMessage());
                        }
                    }
                }, 500);
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onHudShown: " + e.getMessage());
        }
    }

    private void checkHudStatus() {
        try {
            SharedPreferences prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("hud_shown", false)) {
                Log.d(TAG, "HUD status checked - HUD was shown");
                prefs.edit().remove("hud_shown").apply();
                onHudShown();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkHudStatus: " + e.getMessage());
        }
    }

    private void trackAppOpen() {
        if (mFirebaseAnalytics == null) {
            Log.w(TAG, "FirebaseAnalytics is null, skipping app open event");
            return;
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "HomeActivity");
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeActivity");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
            Log.d(TAG, "App open event logged");
        } catch (Exception e) {
            Log.e(TAG, "Error logging app open: " + e.getMessage());
        }
    }

    private void trackButtonClick(String buttonName) {
        if (mFirebaseAnalytics == null) {
            Log.w(TAG, "FirebaseAnalytics is null, skipping button click event");
            return;
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putString("button_name", buttonName);
            bundle.putString("screen", "HomeActivity");
            mFirebaseAnalytics.logEvent("button_click", bundle);
            Log.d(TAG, "Button click: " + buttonName);
        } catch (Exception e) {
            Log.e(TAG, "Error logging button click: " + e.getMessage());
        }
    }

    private void trackGameLaunch() {
        if (mFirebaseAnalytics == null) {
            Log.w(TAG, "FirebaseAnalytics is null, skipping game launch event");
            return;
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("cache_installed", isCacheInstalled());
            mFirebaseAnalytics.logEvent("game_launch", bundle);
            Log.d(TAG, "Game launch event logged");
        } catch (Exception e) {
            Log.e(TAG, "Error logging game launch: " + e.getMessage());
        }
    }

    private void initViews() {
        try {
            nicknameEditText = findViewById(R.id.name);
            donateButton = findViewById(R.id.donate_btn);
            logButton = findViewById(R.id.log_btn);
            playButton = findViewById(R.id.play_btn);
            telegramButton = findViewById(R.id.tg_btn);
            vkButton = findViewById(R.id.vk_btn);
            webButton = findViewById(R.id.web_btn);

            if (nicknameEditText == null) Log.w(TAG, "nicknameEditText not found");
            if (donateButton == null) Log.w(TAG, "donateButton not found");
            if (logButton == null) Log.w(TAG, "logButton not found");
            if (playButton == null) Log.w(TAG, "playButton not found");
            if (telegramButton == null) Log.w(TAG, "telegramButton not found");
            if (vkButton == null) Log.w(TAG, "vkButton not found");
            if (webButton == null) Log.w(TAG, "webButton not found");

        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage());
            Toast.makeText(this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¸Ð½Ð¸Ñ†Ð¸Ð°Ð»Ð¸Ð·Ð°Ñ†Ð¸Ð¸ Ð¸Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÐ°", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNickname() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File externalDir = getExternalFilesDir(null);
                    if (externalDir == null) {
                        Log.e(TAG, "External files directory is null");
                        return;
                    }

                    File file = new File(externalDir, "SAMP/settings.ini");
                    if (file.exists()) {
                        mWini = new Wini(file);
                        String nickname = mWini.get("client", "name");
                        if (nickname != null && !nickname.isEmpty()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    nicknameEditText.setText(nickname);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error loading nickname: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        }).start();

        try {
            nicknameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    saveNickname(s.toString());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting text watcher: " + e.getMessage());
        }
    }

    private void saveNickname(String nickname) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File externalDir = getExternalFilesDir(null);
                    if (externalDir == null) {
                        Log.e(TAG, "External files directory is null");
                        return;
                    }

                    File sampDir = new File(externalDir, "SAMP");
                    if (!sampDir.exists()) {
                        sampDir.mkdirs();
                    }

                    File file = new File(sampDir, "settings.ini");
                    if (mWini == null) {
                        mWini = new Wini(file);
                    }
                    mWini.put("client", "name", nickname);
                    mWini.store();
                    Log.d(TAG, "Nickname saved: " + nickname);
                } catch (IOException e) {
                    Log.e(TAG, "Error saving nickname: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error accessing file for save: " + e.getMessage());
                }
            }
        }).start();
    }

    private boolean isCacheInstalled() {
        try {
            File[] possiblePaths = {
                    new File(Environment.getExternalStorageDirectory() + "/Android/data/com.glory.game/files/version.txt"),
                    new File(getExternalFilesDir(null), "version.txt"),
                    new File(getFilesDir(), "version.txt")
            };

            for (File file : possiblePaths) {
                if (file.exists()) {
                    Log.d(TAG, "Cache found at: " + file.getAbsolutePath());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking cache: " + e.getMessage());
            return false;
        }
    }

    private void setupClickListeners() {
        try {
            if (playButton != null) {
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handlePlayButtonClick();
                    }
                });
            }

            if (donateButton != null) {
                donateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleDonateButtonClick(v);
                    }
                });
            }

            if (logButton != null) {
                logButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleLogButtonClick(v);
                    }
                });
            }

            if (telegramButton != null) {
                telegramButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleSocialButtonClick(v, API.Social.TELEGRAM, "telegram_button");
                    }
                });
            }

            if (vkButton != null) {
                vkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleSocialButtonClick(v, API.Social.VK, "vk_button");
                    }
                });
            }

            if (webButton != null) {
                webButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleSocialButtonClick(v, API.Social.WEBSITE, "web_button");
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage());
        }
    }

    private void handlePlayButtonClick() {
        try {
            trackButtonClick("play_button");

            String nickname = nicknameEditText.getText().toString().trim();

            if (nickname.isEmpty()) {
                Toast.makeText(HomeActivity.this,
                        "Ð—Ð°Ð¿Ð¾Ð»Ð½Ð¸Ñ‚Ðµ 'Ð¸Ð¼Ñ' Ð¸ 'Ñ„Ð°Ð¼Ð¸Ð»Ð¸Ñ' (ÐŸÑ€Ð¸Ð¼ÐµÑ€: Ivan_Ivanov)",
                        Toast.LENGTH_LONG).show();
                nicknameEditText.requestFocus();
                return;
            }

            if (!nickname.contains("_")) {
                Toast.makeText(HomeActivity.this,
                        "ÐÐ¸Ðº Ð´Ð¾Ð»Ð¶ÐµÐ½ ÑÐ¾Ð´ÐµÑ€Ð¶Ð°Ñ‚ÑŒ 'Ð¸Ð¼Ñ_Ñ„Ð°Ð¼Ð¸Ð»Ð¸Ñ' (ÐŸÑ€Ð¸Ð¼ÐµÑ€: Ivan_Ivanov)",
                        Toast.LENGTH_LONG).show();
                nicknameEditText.requestFocus();
                return;
            }

            String[] parts = nickname.split("_");
            if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                Toast.makeText(HomeActivity.this,
                        "Ð—Ð°Ð¿Ð¾Ð»Ð½Ð¸Ñ‚Ðµ Ð¸ Ð¸Ð¼Ñ Ð¸ Ñ„Ð°Ð¼Ð¸Ð»Ð¸ÑŽ (ÐŸÑ€Ð¸Ð¼ÐµÑ€: Ivan_Ivanov)",
                        Toast.LENGTH_LONG).show();
                nicknameEditText.requestFocus();
                return;
            }

            hideKeyboard(this);

            if (!isCacheInstalled()) {
                startCacheInstallation(false);
            } else {
                trackGameLaunch();
                Intent serverIntent = new Intent(HomeActivity.this, ChooseServerActivity.class);
                startActivity(serverIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in play button click: " + e.getMessage());
            Toast.makeText(this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¿Ñ€Ð¸ Ð·Ð°Ð¿ÑƒÑÐºÐµ Ð¸Ð³Ñ€Ñ‹", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDonateButtonClick(View v) {
        try {
            trackButtonClick("donate_button");
            animateButtonClick(v);

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(API.Social.DONATE_URL));
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "ÐÐµÑ‚ Ð±Ñ€Ð°ÑƒÐ·ÐµÑ€Ð° Ð´Ð»Ñ Ð¾Ñ‚ÐºÑ€Ñ‹Ñ‚Ð¸Ñ ÑÑÑ‹Ð»ÐºÐ¸", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in donate button click: " + e.getMessage());
        }
    }

    private void handleLogButtonClick(View v) {
        try {
            trackButtonClick("log_button");
            animateButtonClick(v);
            sendLogFile();
        } catch (Exception e) {
            Log.e(TAG, "Error in log button click: " + e.getMessage());
        }
    }

    private void handleSocialButtonClick(View v, String url, String buttonName) {
        try {
            trackButtonClick(buttonName);
            animateButtonClick(v);

            Intent link = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (link.resolveActivity(getPackageManager()) != null) {
                startActivity(link);
            } else {
                Toast.makeText(this, "ÐÐµÑ‚ Ð¿Ñ€Ð¸Ð»Ð¾Ð¶ÐµÐ½Ð¸Ñ Ð´Ð»Ñ Ð¾Ñ‚ÐºÑ€Ñ‹Ñ‚Ð¸Ñ ÑÑÑ‹Ð»ÐºÐ¸", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in social button click: " + e.getMessage());
        }
    }

    private void animateButtonClick(View v) {
        v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    }
                })
                .start();
    }

    private void sendLogFile() {
        try {
            File externalDir = getExternalFilesDir(null);
            if (externalDir == null) {
                Toast.makeText(this, "ÐÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿Ð½Ð¾ Ð²Ð½ÐµÑˆÐ½ÐµÐµ Ñ…Ñ€Ð°Ð½Ð¸Ð»Ð¸Ñ‰Ðµ", Toast.LENGTH_SHORT).show();
                return;
            }

            File logFile = new File(externalDir, "logcat.txt");

            if (!logFile.exists()) {
                Toast.makeText(this, "Ð¤Ð°Ð¹Ð» logcat.txt Ð½Ðµ Ð½Ð°Ð¹Ð´ÐµÐ½", Toast.LENGTH_SHORT).show();
                return;
            }

            if (logFile.length() == 0) {
                Toast.makeText(this, "Ð¤Ð°Ð¹Ð» logcat.txt Ð¿ÑƒÑÑ‚", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    logFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SAMP Log File");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Ð›Ð¾Ð³ Ñ„Ð°Ð¹Ð» Ð¸Ð· Ð¸Ð³Ñ€Ñ‹");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(shareIntent, "ÐžÑ‚Ð¿Ñ€Ð°Ð²Ð¸Ñ‚ÑŒ logcat.txt Ñ‡ÐµÑ€ÐµÐ·:");

            if (chooserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooserIntent);
            } else {
                Toast.makeText(this, "ÐÐµÑ‚ Ð¿Ñ€Ð¸Ð»Ð¾Ð¶ÐµÐ½Ð¸Ð¹ Ð´Ð»Ñ Ð¾Ñ‚Ð¿Ñ€Ð°Ð²ÐºÐ¸ Ñ„Ð°Ð¹Ð»Ð¾Ð²", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending log file: " + e.getMessage());
            Toast.makeText(this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¿Ñ€Ð¸ Ð¾Ñ‚Ð¿Ñ€Ð°Ð²ÐºÐµ Ð»Ð¾Ð³Ð°", Toast.LENGTH_SHORT).show();
        }
    }

    private void trackCacheInstallStarted() {
        if (mFirebaseAnalytics == null) {
            Log.w(TAG, "FirebaseAnalytics is null, skipping cache install event");
            return;
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putString("install_type", "cache_installation");
            mFirebaseAnalytics.logEvent("install_started", bundle);
            Log.d(TAG, "Cache install started");
        } catch (Exception e) {
            Log.e(TAG, "Error logging install start: " + e.getMessage());
        }
    }

    public static void hideKeyboard(Activity activity) {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            View currentFocusedView = activity.getCurrentFocus();
            if (currentFocusedView != null) {
                inputManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            handler.removeCallbacksAndMessages(null);
            if (loadingScreen != null) {
                loadingScreen.destroy();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
}
