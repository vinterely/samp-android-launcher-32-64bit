package com.glory.game.ui;
import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.app.Activity;
import android.widget.*;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.glory.game.R;
import com.glory.launcher.util.Util;
import com.glory.launcher.util.SeekArc;
import com.nvidia.devtech.NvEventQueueActivity;

public class Speedometer {
    public static boolean s_bEnableSpeedometer = false;
    private Activity activity;
    private NvEventQueueActivity nvActivity;

    private ConstraintLayout mSpeedLayout;
    private SeekArc mSpeedLine;
    private SeekArc mSpeedLine1;
    private TextView mSpeedText;

    private SeekArc mFuelLine;
    private SeekArc mFuelLine2;
    private ImageView mFuelType;
    private TextView mFuelText;

    private SeekArc mHpLine;
    private TextView mHpText;

    private TextView mMilleageText;

    private ImageView mSpeedEngine;
    private ImageView mSpeedLights;
    private ImageView mSpeedDoors;
    private ImageView mEngineStart;

    private static Speedometer instance;

    public int povorright, povorleft;
    public ImageView datpovorleft, datpovorright;
    public ImageView povorotnikleft, povorotnikright;
    private boolean isLeftBlinking = false, isRightBlinking = false;
    private final Handler handler = new Handler();
    private Runnable leftBlinker, rightBlinker;

    private boolean mKeyboardVisible = false;

    public Speedometer(Activity activity) {
        this.activity = activity;

        if (activity instanceof NvEventQueueActivity) {
            this.nvActivity = (NvEventQueueActivity) activity;
        } else {
            Log.e("Speedometer", "Activity is not an instance of NvEventQueueActivity");
            this.nvActivity = null;
        }

        instance = this;
        initializeViews();
        setupKeyboardListener();
        setupEngineStartListener();

        if (mSpeedLayout != null) {
            Util.HideLayout(mSpeedLayout, false);
        }

    }

    private void initializeViews() {
        try {
            mSpeedLayout = activity.findViewById(R.id.speed_layout);

            mSpeedLine1 = activity.findViewById(R.id.speed_line1);
            mSpeedLine = activity.findViewById(R.id.speed_line);
            mEngineStart = activity.findViewById(R.id.engine_start);
            mSpeedText = activity.findViewById(R.id.speed_text);

            mFuelLine2 = activity.findViewById(R.id.fuel_line2);
            mFuelLine = activity.findViewById(R.id.fuel_line);
            mFuelType = activity.findViewById(R.id.fuel_type);
            mFuelText = activity.findViewById(R.id.fuel_text);

            mHpLine = activity.findViewById(R.id.hp_line);
            mHpText = activity.findViewById(R.id.hp_text);

            mMilleageText = activity.findViewById(R.id.milleage_text);

            mSpeedEngine = activity.findViewById(R.id.speed_engine);
            mSpeedLights = activity.findViewById(R.id.speed_lights);
            mSpeedDoors = activity.findViewById(R.id.speed_doors);


        } catch (Exception e) {
            Log.e("Speedometer", "Error initializing views: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEngineStartListener() {
        if (mEngineStart != null) {
            mEngineStart.setOnClickListener(new View.OnClickListener() {
                private long lastEngineToggleTick = 0;
                @Override
                public void onClick(View v) {
                    long now = System.currentTimeMillis();
                    if (now - lastEngineToggleTick < 700) {
                        return;
                    }
                    lastEngineToggleTick = now;

                    v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                    byte[] commandBytes = "/en".getBytes();
                    if (nvActivity != null) {
                        nvActivity.sendCommand(commandBytes);
                    } else {
                        try {
                            NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                        } catch (Exception e) {
                            Log.e("Speedometer", "Failed to send command: " + e.getMessage());
                        }
                    }
                }
            });

            mEngineStart.setClickable(true);
            mEngineStart.setFocusable(true);

        } else {
            Log.w("Speedometer", "Engine start button not found");
        }
    }

    private void setupKeyboardListener() {
        try {
            final View activityRootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);

            activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                private boolean wasKeyboardOpened = false;
                private final int defaultKeyboardHeightDP = 100;
                private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
                private final Rect rect = new Rect();

                @Override
                public void onGlobalLayout() {
                    activityRootView.getWindowVisibleDisplayFrame(rect);

                    int screenHeight = activityRootView.getRootView().getHeight();
                    int keypadHeight = screenHeight - rect.bottom;

                    float scale = activity.getResources().getDisplayMetrics().density;
                    int keypadHeightDP = (int) (keypadHeight / scale);

                    boolean isKeyboardNowVisible = keypadHeightDP >= EstimatedKeyboardDP;

                    if (isKeyboardNowVisible == wasKeyboardOpened) {
                        return;
                    }

                    wasKeyboardOpened = isKeyboardNowVisible;
                    mKeyboardVisible = isKeyboardNowVisible;

                    if (isKeyboardNowVisible) {
                        HideSpeed();
                    } else {
                    }
                }
            });
        } catch (Exception e) {
            Log.e("Speedometer", "Error setting up keyboard listener: " + e.getMessage());
        }
    }

    private boolean isKeyboardVisible() {
        return mKeyboardVisible;
    }

    public static void UpdateFromNative(int speed, int fuel, int hp, int mileage,
                                        int engine, int light, int belt, int lock) {
        if (instance != null) {
            instance.UpdateSpeedInfo(speed, fuel, hp, mileage, engine, light, belt, lock);
        } else {
            Log.w("Speedometer", "Instance is null - cannot update");
        }
    }

    public static void NativeUpdateSpeedometer(int speed, int fuel, int hp, int mileage,
                                               int engine, int light, int belt, int lock) {
        UpdateFromNative(speed, fuel, hp, mileage, engine, light, belt, lock);
    }

    @SuppressLint("SetTextI18n")
    public void UpdateSpeedInfo(int speed, int fuel, int hp, int mileage,
                                int engine, int light, int belt, int lock) {
        if (!s_bEnableSpeedometer) {
            if (mSpeedLayout != null && mSpeedLayout.getVisibility() == View.VISIBLE) {
                HideSpeed();
            }
            return;
        }

        if (mSpeedLayout != null && mSpeedLayout.getVisibility() != View.VISIBLE && !isKeyboardVisible()) {
            ShowSpeed();
        }

        if (speed > 500) speed = 500;
        if (speed < 0) speed = 0;

        if (fuel > 100) fuel = 100;
        if (fuel < 0) fuel = 0;

        if (mSpeedText != null) {
            mSpeedText.setText(String.valueOf(speed));
        }

        if (mSpeedLine != null) {
            mSpeedLine.setProgress(speed);
        }

        if (mFuelText != null) {
            mFuelText.setText(String.valueOf(fuel));
        }

        if (mFuelLine != null) {
            mFuelLine.setProgress(fuel);
        }

        if (mHpText != null) {
            mHpText.setText(String.valueOf(hp));
        }

        if (mHpLine != null) {
            // Assuming hp is passed as percentage (0-100) or we rely on max value of SeekArc
            // If raw HP (1000) is passed, we might need to verify or scale
            mHpLine.setProgress(hp);
        }

        if (mMilleageText != null) {
            mMilleageText.setText(String.valueOf(mileage));
        }

        if (mSpeedEngine != null) {
            mSpeedEngine.setImageResource(engine == 1 ?
                    R.drawable.ico_engine_on : R.drawable.ico_engine_off);
        }

        if (mSpeedLights != null) {
            mSpeedLights.setImageResource(light == 1 ?
                    R.drawable.ico_lights_on : R.drawable.ico_lights_off);
        }

        if (mSpeedDoors != null) {
            mSpeedDoors.setImageResource(lock == 1 ?
                    R.drawable.ico_lock_on : R.drawable.ico_lock_off);
        }

        if (belt == 1 && mSpeedDoors != null) {
            mSpeedDoors.setImageResource(R.drawable.ico_lock_off);
        }
    }

    public void ShowSpeed() {
        if (!s_bEnableSpeedometer) {
            HideSpeed();
            return;
        }
        if (mSpeedLayout != null && !isKeyboardVisible()) {
            Util.ShowLayout(mSpeedLayout, false);
        }
    }

    public void HideSpeed() {
        if (mSpeedLayout != null) {
            Util.HideLayout(mSpeedLayout, false);
        }
    }

    public ConstraintLayout getSpeedLayout() {
        return this.mSpeedLayout;
    }

    public boolean isVisible() {
        return mSpeedLayout != null && mSpeedLayout.getVisibility() == View.VISIBLE;
    }

    private void startLeftBlinker() {
    }

    private void startRightBlinker() {
    }

    private void stopBlinking() {
    }

    public void SetProbeg(float probeg) {
        if (mMilleageText != null) {
            mMilleageText.setText(String.valueOf(probeg));
        }
    }

    public static void cleanup() {
        if (instance != null) {
            instance.handler.removeCallbacksAndMessages(null);
        }
        instance = null;
    }
}
