package com.glory.game;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.joom.paranoid.Obfuscate;
import com.glory.game.ui.AttachEdit;
import com.glory.game.ui.CustomKeyboard;
import com.glory.game.ui.LoadingScreen;
import com.glory.game.ui.dialog.DialogManager;
import com.glory.launcher.util.SignatureChecker;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Obfuscate
public class SAMP extends GTASA implements CustomKeyboard.InputListener, HeightProvider.HeightListener {
    private static final String TAG = "SAMP";
    private static SAMP instance;

    private CustomKeyboard mKeyboard;
    private DialogManager mDialog;
    private HeightProvider mHeightProvider;

    private AttachEdit mAttachEdit;
    private LoadingScreen mLoadingScreen;

    private boolean isInitializing = false;
    private boolean isInitialized = false;
    private Handler mHandler = new Handler();

    public native void sendDialogResponse(int i, int i2, int i3, byte[] str);

    public static SAMP getInstance() {
        return instance;
    }

    private void hideTab() {
    }

    private void setTab(int id, String name, int score, int ping) {
    }

    private void clearTab() {
    }

    private void showLoadingScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLoadingScreen != null) {
                    mLoadingScreen.show();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mLoadingScreen != null && mLoadingScreen.isVisible()) {
                                mLoadingScreen.hide();
                            }
                        }
                    }, 50000);
                }
            }
        });
    }

    private void hideLoadingScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLoadingScreen != null && mLoadingScreen.isVisible()) {
                    mLoadingScreen.hide();
                }
            }
        });
    }

    public void setPauseState(boolean pause) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pause) {
                    if (mDialog != null) {
                        mDialog.hideWithoutReset();
                    }
                    if (mAttachEdit != null) {
                        mAttachEdit.hideWithoutReset();
                    }
                } else {
                    if (mDialog != null && mDialog.isShow) {
                        mDialog.showWithOldContent();
                    }
                    if (mAttachEdit != null && mAttachEdit.isShow) {
                        mAttachEdit.showWithoutReset();
                    }
                }
            }
        });
    }

    public void exitGame() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

        finishAndRemoveTask();
        System.exit(0);
    }

    public void showDialog(int dialogId, int dialogTypeId, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        final String caption = new String(bArr, StandardCharsets.UTF_8);
        final String content = new String(bArr2, StandardCharsets.UTF_8);
        final String leftBtnText = new String(bArr3, StandardCharsets.UTF_8);
        final String rightBtnText = new String(bArr4, StandardCharsets.UTF_8);
        runOnUiThread(() -> {
            if (mDialog != null) {
                this.mDialog.show(dialogId, dialogTypeId, caption, content, leftBtnText, rightBtnText);
            }
        });
    }

    private native void onInputEnd(byte[] str);

    @Override
    public void OnInputEnd(String str) {
        byte[] toReturn = null;
        try {
            toReturn = str.getBytes("windows-1251");
        } catch (UnsupportedEncodingException e) {
        }

        try {
            onInputEnd(toReturn);
        } catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }
    }

    private void showKeyboard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("AXL", "showKeyboard()");
                if (mKeyboard != null) {
                    mKeyboard.ShowInputLayout();
                }
            }
        });
    }

    private void hideKeyboard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mKeyboard != null) {
                    mKeyboard.HideInputLayout();
                }
            }
        });
    }

    private void showEditObject() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAttachEdit != null) {
                    mAttachEdit.show();
                }
            }
        });
    }

    private void hideEditObject() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAttachEdit != null) {
                    mAttachEdit.hide();
                }
            }
        });
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }
    }

    private void performRealSampInitialization() {
        if (isInitializing || isInitialized) {
            return;
        }

        isInitializing = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Loading...");

                    Log.d(TAG, "Loading...");
                    Thread.sleep(25000);

                    Log.d(TAG, "Loading...");
                    try {
                        initializeSAMP();
                    } catch (Exception e) {
                        Log.e(TAG, "Error calling initializeSAMP: " + e.getMessage());
                    }

                    Thread.sleep(25000);

                    Log.d(TAG, "Loading...");
                    Thread.sleep(0);

                    Log.d(TAG, "Loading...");
                    Thread.sleep(0);

                    Log.d(TAG, "Loading");
                    isInitialized = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSharedPreferences("game_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("hud_shown", true)
                                    .apply();
                        }
                    });

                } catch (InterruptedException e) {
                    Log.e(TAG, "Ð˜Ð½Ð¸Ñ†Ð¸Ð°Ð»Ð¸Ð·Ð°Ñ†Ð¸Ñ Ð¿Ñ€ÐµÑ€Ð²Ð°Ð½Ð°: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Initialization error: " + e.getMessage());
                } finally {
                    isInitializing = false;
                }
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "**** onCreate");

        setFullScreenMode();

        super.onCreate(savedInstanceState);

        if (SignatureChecker.isSignatureValid(this, getPackageName())) {
            Toast.makeText(this, "Use original launcher! No remake", Toast.LENGTH_LONG).show();
            return;
        }

        mKeyboard = new CustomKeyboard(this);
        mDialog = new DialogManager(this);
        mAttachEdit = new AttachEdit(this);
        mLoadingScreen = new LoadingScreen(this);

        instance = this;

        showLoadingScreen();

        performRealSampInitialization();

        try {
            initializeSAMP();
        } catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }
    }

    private native void initializeSAMP();

    @Override
    public void onStart() {
        Log.i(TAG, "**** onStart");
        super.onStart();
    }

    @Override
    public void onRestart() {
        Log.i(TAG, "**** onRestart");
        super.onRestart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "**** onResume");
        super.onResume();

        setFullScreenMode();
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

    public native void onEventBackPressed();

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEventBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onEventBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "**** onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "**** onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "**** onDestroy");
        super.onDestroy();

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        if (mLoadingScreen != null) {
            mLoadingScreen.destroy();
        }

        instance = null;
    }

    @Override
    public void onHeightChanged(int orientation, int height) {
    }
}