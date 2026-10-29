package com.glory.game.ui;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ToggleButton;
import com.glory.launcher.Preferences;



import androidx.constraintlayout.widget.ConstraintLayout;



import com.nvidia.devtech.NvEventQueueActivity;
import com.glory.Config;
import com.glory.game.R;



import org.ini4j.Wini;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



import android.view.WindowManager;



public class Hud {
    private static final boolean CTR_DEBUG = false;



    public static final int HUD_HP = 0;
    public static final int HUD_ARMOR = 1;
    public static final int HUD_MONEY = 2;
    public static final int HUD_WANTED = 3;
    public static final int HUD_HP_TEXT = 4;
    public static final int HUD_ARMOR_TEXT = 5;
    public static final int HUD_RADAR = 6;
    public static final int HUD_CHAT = 7;
    public static final int HUD_FIST = 8;
    public static final int HUD_AMMO = 9;
    public static final int HUD_EAT = 10;
    public static final int HUD_TAB = 11;
    public static final int HUD_KILL_LIST = 12;
    public static final int HUD_SHOOT_BTN = 13;
    public static final int HUD_VOICE_BTN = 14;
    public static final int HUD_ENTER_CAR_BTN = 15;
    public static final int HUD_CAMERA_BTN = 16;
    public static final int HUD_ELEMENTS = 20;
    public float[] hudSensitivity = new float[HUD_ELEMENTS];
    public boolean[] hudEnabled = new boolean[HUD_ELEMENTS];



    public Activity activity;
    public ConstraintLayout main_hud;
    private final NvEventQueueActivity mContext = null;
    private boolean isHudSetPos = false;



    private NvEventQueueActivity nvActivity;



    public ConstraintLayout hud_info_layout, health_layout, armour_layout, eat_layout;
    public TextView health_text, armour_text, eat_text, hud_money, hud_ammo, hud_logo_text;
    public ImageView hud_logo_img;
    public ConstraintLayout samp_buttons_layout;
    public ImageView hud_button_menu, hud_button_star, hud_button_inv, hud_button_shop, hud_button_help;
    public ImageView radar_zone;
    public ImageView radar_bg;
    public ConstraintLayout radar_layout, select_weapon, weapon_layout;
    public ImageView weapon_button;
    public ImageView star_1, star_2, star_3, star_4, star_5;
    public ConstraintLayout hud_logo_layout;

    private boolean mIsVehicleMode = false;



    private Context context;
    private final Handler handler = new Handler();



    private String playerName = "Daler_Baltaev";
    private int playerId = 0;
    private int currentWeaponId = 0;
    private int currentAmmo = 0;
    private int currentAmmoInClip = 0;
    private boolean isFirstShow = true;



    public final float[] hudPosX = new float[HUD_ELEMENTS];
    public final float[] hudPosY = new float[HUD_ELEMENTS];
    public final float[] hudScaleX = new float[HUD_ELEMENTS];
    public final float[] hudScaleY = new float[HUD_ELEMENTS];



    private int[] getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            activity.getDisplay().getRealMetrics(metrics);
        } else {
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
        }
        int w = Math.max(metrics.widthPixels, metrics.heightPixels);
        int h = Math.min(metrics.widthPixels, metrics.heightPixels);
        return new int[]{w, h};
    }



    private class ConvertViewCoordsToGta {
        class Data {
            float x, y, width, height;



            Data(float x, float y, float width, float height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }



        Data convertCoordsToGta(Data data) {
            int[] screenSize = Hud.this.getScreenSize();
            float screenWidth = (float) screenSize[0];
            float screenHeight = (float) screenSize[1];



            Data returnData = new Data(0, 0, 0, 0);



            float viewPercentX = data.x / screenWidth;
            float viewPercentY = data.y / screenHeight;



            returnData.x = 640f * viewPercentX;
            returnData.y = 480f * viewPercentY;



            float viewPercentWidth = data.width / screenWidth;
            float viewPercentHeight = data.height / screenHeight;



            returnData.width = 640f * viewPercentWidth * 0.895f;
            returnData.height = 480f * viewPercentHeight;



            return returnData;
        }
    }



    native void SetRadarBgPos(float x1, float y1, float x2, float y2);
    native void nativeSetRadarPos(float x, float y, float width, float height);
    public native void setNativeHudRadar(float centerX, float centerY, float radiusX, float radiusY);
    public native void nativeOpenMap();
    public native void setNativeWidgetShoot(float x, float y, float size, float sensitivity, boolean enabled);
    public native void sendCtrlKey();
    public native void sendAltKey();
    public native void sendTabKey();
    public native void cycleWeapon();
    public native void sendGKey();
    public native void sendYKey();
    public native void sendNKey();
    public native void sendHKey();
    public native void clickCameraMode();
    public native void setNativeChatConfig(float xPercent, float yPercent, float widthPercent, int lines, float fontSize);
    public native void setNativeHudMode(boolean isClassic);

    public boolean isClassicHud() {
        try {
            if (activity != null) {
                String hudType = Preferences.getString(activity, Preferences.HUD_TYPE, "");
                if ("classic".equalsIgnoreCase(hudType)) return true;
                if ("glory".equalsIgnoreCase(hudType)) return false;
            }
        } catch (Exception ignored) {}

        try {
            File iniFile = getSettingsFile();
            if (iniFile != null && iniFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(iniFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim().toLowerCase();
                        if (line.startsWith("hud_type")) {
                            String[] parts = line.split("=");
                            if (parts.length >= 2) {
                                String val = parts[1].trim();
                                if (val.contains("classic")) return true;
                                if (val.contains("glory")) return false;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public Hud(Activity aactivity) {
        activity = aactivity;
        main_hud = aactivity.findViewById(R.id.main_hud);
        try {
            nvActivity = NvEventQueueActivity.getInstance();
        } catch (Exception e) {
            Log.e("HUD", "Error getting NvEventQueueActivity instance: " + e.getMessage());
        }



        if (main_hud == null) {
            Log.e("Hud", "Main HUD layout is null! Check setContentView.");
            return;
        }



        initializeViews();



        loadPlayerNameFromSettings();



        setupRadarPosition();



        loadHudConfig();
        applyHudConfig();
        try {
            setNativeHudMode(isClassicHud());
        } catch (Throwable ignored) {}

        HideHud();



        setInitialValues();



        setupButtonClickListeners();
    }



    private void setupButtonClickListeners() {
        setMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));



                byte[] commandBytes = "/mm".getBytes();



                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });



        setStarButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));
                sendTabKey();
            }
        });



        setInvButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));



                byte[] commandBytes = "/inv".getBytes();



                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });



        setShopButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));
                if (CTR_DEBUG) {
                    Log.i("CTR_DBG", "[CTR_DBG] CTR button tapped -> calling sendCtrlKey() exactly once");
                }
                sendCtrlKey();
            }
        });



        setHelpButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));
                sendAltKey();
                byte[] commandBytes = "/alt".getBytes();



                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });



        if (weapon_button != null) {
            weapon_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));
                    cycleWeapon();
                }
            });
        }



        if (select_weapon != null) {
            select_weapon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));
                    cycleWeapon();
                }
            });
        }
    }



    private void loadPlayerNameFromSettings() {
        try {
            File file = getSettingsFile();



            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;



                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("name")) {
                        String[] parts = line.split("=");
                        if (parts.length >= 2) {
                            playerName = parts[1].trim();
                            Log.d("HUD", "Loaded player name from settings: " + playerName);
                            break;
                        }
                    }
                }
                reader.close();



                if (playerName.equals("Daler_Baltaev")) {
                    reader = new BufferedReader(new FileReader(file));
                    boolean inClientSection = false;



                    while ((line = reader.readLine()) != null) {
                        line = line.trim();



                        if (line.startsWith("[") && line.endsWith("]")) {
                            inClientSection = line.equalsIgnoreCase("[client]");
                            continue;
                        }



                        if (inClientSection && line.startsWith("name")) {
                            String[] parts = line.split("=");
                            if (parts.length >= 2) {
                                playerName = parts[1].trim();
                                Log.d("HUD", "Loaded player name from [client] section: " + playerName);
                                break;
                            }
                        }
                    }
                    reader.close();
                }
            } else {
                Log.e("HUD", "SAMP settings file not found: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e("HUD", "Error reading SAMP settings: " + e.getMessage());
            playerName = "Daler_Baltaev";
        }



        if (playerName.equals("Daler_Baltaev")) {
            try {
                File file = getSettingsFile();
                if (file.exists()) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.toLowerCase().contains("name") && line.contains("=")) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                playerName = parts[1].trim();
                                Log.d("HUD", "Fallback loaded player name: " + playerName);
                                break;
                            }
                        }
                    }
                    br.close();
                }
            } catch (Exception e) {
                Log.e("HUD", "Fallback error: " + e.getMessage());
            }
        }
    }



    private void initializeViews() {
        hud_info_layout = main_hud.findViewById(R.id.hud_info_layout);
        health_layout = main_hud.findViewById(R.id.health_layout);
        armour_layout = main_hud.findViewById(R.id.armour_layout);
        eat_layout = main_hud.findViewById(R.id.eat_layout);
        health_text = main_hud.findViewById(R.id.health_text);
        armour_text = main_hud.findViewById(R.id.armour_text);
        eat_text = main_hud.findViewById(R.id.eat_text);
        hud_money = main_hud.findViewById(R.id.hud_money);
        samp_buttons_layout = main_hud.findViewById(R.id.samp_buttons_layout);
        hud_button_menu = main_hud.findViewById(R.id.hud_button_menu);
        hud_button_star = main_hud.findViewById(R.id.hud_button_star);
        hud_button_inv = main_hud.findViewById(R.id.hud_button_inv);
        hud_button_shop = main_hud.findViewById(R.id.hud_button_shop);
        hud_button_help = main_hud.findViewById(R.id.hud_button_help);
        radar_zone = main_hud.findViewById(R.id.radar_zone);
        radar_bg = main_hud.findViewById(R.id.imageView12);
        radar_layout = main_hud.findViewById(R.id.radar_layout);
        weapon_layout = main_hud.findViewById(R.id.weapon_layout);
        select_weapon = main_hud.findViewById(R.id.select_weapon);
        weapon_button = main_hud.findViewById(R.id.weapon_button);



        if (select_weapon != null) select_weapon.setClickable(true);
        if (weapon_button != null) weapon_button.setClickable(true);
        if (weapon_layout != null) weapon_layout.bringToFront();



        hud_ammo = main_hud.findViewById(R.id.hud_ammo);
        star_1 = main_hud.findViewById(R.id.star_1);
        star_2 = main_hud.findViewById(R.id.star_2);
        star_3 = main_hud.findViewById(R.id.star_3);
        star_4 = main_hud.findViewById(R.id.star_4);
        star_5 = main_hud.findViewById(R.id.star_5);
        hud_logo_layout = main_hud.findViewById(R.id.hud_logo_layout);
        hud_logo_img = main_hud.findViewById(R.id.hud_logo_img);
        hud_logo_text = main_hud.findViewById(R.id.hud_logo_text);

    }



    private void setInitialValues() {
        if (health_text != null) health_text.setText("0%");
        if (armour_text != null) armour_text.setText("0%");
        if (eat_text != null) eat_text.setText("100%");
        if (hud_money != null) hud_money.setText("$ 0");
        if (hud_ammo != null) hud_ammo.setText("0/0");



        updateWantedStars(0);

        updateLogoInfo();
    }



    public void updateRadarToCircleFrame() {
        if (isClassicHud()) {
            setNativeHudRadar(50.0f, 70.0f, 45.0f, 45.0f);
            return;
        }

        View target = (radar_bg != null) ? radar_bg : radar_layout;
        if (target == null) return;

        final View fTarget = target;
        Runnable updateAction = () -> {
            int[] loc = new int[2];
            fTarget.getLocationOnScreen(loc);
            int[] screen = getScreenSize();
            if (screen[0] <= 0 || screen[1] <= 0) return;

            View parentLayout = radar_layout;
            float parentScaleX = (parentLayout != null) ? parentLayout.getScaleX() : 1f;
            float parentScaleY = (parentLayout != null) ? parentLayout.getScaleY() : 1f;
            float effectiveScaleX = parentScaleX * fTarget.getScaleX();
            float effectiveScaleY = parentScaleY * fTarget.getScaleY();

            float w = fTarget.getWidth() * effectiveScaleX;
            float h = fTarget.getHeight() * effectiveScaleY;
            if (w <= 0 || h <= 0) return;

            float centerX = loc[0] + (w / 2f);
            float centerY = loc[1] + (h / 2f);

            // Cutout ratio inside bg_weapon.png is 161/182 (~0.8846)
            float radius = (w / 2f) * (161.0f / 182.0f);

            // GTA SA Mobile virtual coordinate space:
            // Base virtual height is fixed at 448.0f virtual units.
            // RSGlobal multiplier is (screenHeight / 448.0f).
            // Converting screen pixels using mult = 448.0f / screenHeight ensures 100% exact 1:1 pixel sync.
            float mult = 448.0f / (float) screen[1];
            float gtaCenterX = centerX * mult;
            float gtaCenterY = centerY * mult;
            float gtaRadiusX = radius * mult;
            float gtaRadiusY = radius * mult;

            setNativeHudRadar(gtaCenterX, gtaCenterY, gtaRadiusX, gtaRadiusY);
        };

        if (target.getWidth() > 0 && target.getHeight() > 0) {
            updateAction.run();
        } else {
            target.post(updateAction);
        }
    }

    private void setupRadarPosition() {
        View target = (radar_bg != null) ? radar_bg : radar_layout;
        if (target != null) {
            target.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    updateRadarToCircleFrame();
                }
            });
        }
        if (radar_layout != null && radar_layout != target) {
            radar_layout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    updateRadarToCircleFrame();
                }
            });
        }
        updateRadarToCircleFrame();
        setupRadarTouch();
    }
    private void setupRadarTouch() {
        View touchView = (radar_zone != null) ? radar_zone : radar_bg;
        if (touchView == null) return;
        touchView.setClickable(true);
        touchView.setFocusable(false);
        touchView.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private boolean isDownInside = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (NvEventQueueActivity.getInstance() != null &&
                    NvEventQueueActivity.getInstance().isHudEditorShowing()) {
                    return false;
                }
                float w = v.getWidth();
                float h = v.getHeight();
                if (w <= 0 || h <= 0) return false;
                float cX = w / 2f;
                float cY = h / 2f;
                float radius = (w / 2f) * (161.0f / 182.0f);
                float x = event.getX();
                float y = event.getY();
                float dx = x - cX;
                float dy = y - cY;
                boolean insideCircle = (dx * dx + dy * dy) <= (radius * radius);
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (insideCircle) {
                            downX = x;
                            downY = y;
                            isDownInside = true;
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        return isDownInside;
                    case MotionEvent.ACTION_UP:
                        if (isDownInside) {
                            isDownInside = false;
                            float moveDistSq = (x - downX) * (x - downX) + (y - downY) * (y - downY);
                            float maxMovePx = 30f * v.getResources().getDisplayMetrics().density;
                            if (moveDistSq <= maxMovePx * maxMovePx && insideCircle) {
                                openMap();
                            }
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        isDownInside = false;
                        return false;
                }
                return false;
            }
        });
    }
    public void openMap() {
        Log.d("HUD", "Radar touched -> opening map");
        try {
            nativeOpenMap();
        } catch (UnsatisfiedLinkError e) {
            Log.e("HUD", "nativeOpenMap link error", e);
        }
    }



    private File getSettingsFile() {
        return new File(Config.GAME_PATH, "SAMP/settings.ini");
    }



    private void resetHudConfigValues() {
        for (int i = 0; i < HUD_ELEMENTS; i++) {
            hudPosX[i] = -1f;
            hudPosY[i] = -1f;
            hudScaleX[i] = -1f;
            hudScaleY[i] = -1f;
            hudSensitivity[i] = 0f;
            hudEnabled[i] = true;
        }

        hudPosX[HUD_CHAT] = 30f;
        hudPosY[HUD_CHAT] = 27f;
        hudScaleX[HUD_CHAT] = 45f;
        hudScaleY[HUD_CHAT] = 0f;
        hudSensitivity[HUD_CHAT] = 20f;
    }



    public void loadHudConfig() {
        resetHudConfigValues();



        File file = getSettingsFile();
        if (!file.exists()) return;



        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inHudSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    inHudSection = line.equalsIgnoreCase("[hud]");
                    continue;
                }
                if (inHudSection && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim().toLowerCase();
                    String val = parts[1].trim();
                    try {
                        float fVal = Float.parseFloat(val);
                        for (int i = 0; i < HUD_ELEMENTS; i++) {
                            if (key.equals("hud_position_x_" + i)) hudPosX[i] = fVal;
                            else if (key.equals("hud_position_y_" + i)) hudPosY[i] = fVal;
                            else if (key.equals("hud_scale_x_" + i)) hudScaleX[i] = fVal;
                            else if (key.equals("hud_scale_y_" + i)) hudScaleY[i] = fVal;
                            else if (key.equals("hud_sensitivity_" + i)) hudSensitivity[i] = fVal;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            Log.d("HUD", "HUD config loaded successfully from " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("HUD", "Error loading HUD config: " + e.getMessage());
        }
    }



    public synchronized void saveHudConfig() {
        File file = getSettingsFile();
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }



        List<String> outputLines = new ArrayList<>();
        boolean inHudSection = false;



        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        inHudSection = trimmed.equalsIgnoreCase("[hud]");
                        if (inHudSection) {
                            continue;
                        }
                    }
                    if (!inHudSection) {
                        outputLines.add(line);
                    }
                }
            } catch (Exception e) {
                Log.e("HUD", "Error reading settings file for save: " + e.getMessage());
            }
        }



        outputLines.add("");
        outputLines.add("[hud]");
        for (int i = 0; i < HUD_ELEMENTS; i++) {
            if (hudPosX[i] >= 0f) outputLines.add("hud_position_x_" + i + " = " + Math.round(hudPosX[i]));
            else outputLines.add("hud_position_x_" + i + " = -1");



            if (hudPosY[i] >= 0f) outputLines.add("hud_position_y_" + i + " = " + Math.round(hudPosY[i]));
            else outputLines.add("hud_position_y_" + i + " = -1");



            if (hudScaleX[i] > 0f) outputLines.add("hud_scale_x_" + i + " = " + Math.round(hudScaleX[i]));
            else outputLines.add("hud_scale_x_" + i + " = -1");



            if (hudScaleY[i] > 0f) outputLines.add("hud_scale_y_" + i + " = " + Math.round(hudScaleY[i]));
            else outputLines.add("hud_scale_y_" + i + " = -1");

            if (hudSensitivity[i] > 0f) outputLines.add("hud_sensitivity_" + i + " = " + Math.round(hudSensitivity[i]));
            else outputLines.add("hud_sensitivity_" + i + " = -1");
        }



        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String l : outputLines) {
                writer.write(l);
                writer.newLine();
            }
            Log.d("HUD", "HUD config saved successfully to " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("HUD", "Error saving HUD config: " + e.getMessage());
        }
    }



    public synchronized void resetHudConfig() {
        resetHudConfigValues();
        applyAllHudElements();



        File file = getSettingsFile();
        if (!file.exists()) return;



        List<String> outputLines = new ArrayList<>();
        boolean inHudSection = false;



        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inHudSection = trimmed.equalsIgnoreCase("[hud]");
                    if (inHudSection) continue;
                }
                if (!inHudSection) {
                    outputLines.add(line);
                }
            }
        } catch (Exception e) {
            Log.e("HUD", "Error reading settings file for reset: " + e.getMessage());
        }



        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String l : outputLines) {
                writer.write(l);
                writer.newLine();
            }
            Log.d("HUD", "HUD config reset successfully in " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("HUD", "Error resetting HUD config: " + e.getMessage());
        }
    }



    public void applyHudConfig() {
        if (main_hud == null) return;
        main_hud.post(this::applyAllHudElements);
    }



    private void applyAllHudElements() {
        if (isClassicHud()) {
            if (main_hud != null) main_hud.setVisibility(View.GONE);
            setNativeHudRadar(50.0f, 70.0f, 45.0f, 45.0f);
            setNativeChatConfig(0.17f, 0.025f, 0.55f, 8, 20.0f);
            return;
        }

        if (main_hud != null) main_hud.setVisibility(View.VISIBLE);
        applyHealthHud();
        applyArmorHud();
        applySimpleElement(HUD_MONEY, hud_money);
        applyWantedHud();
        applyWeaponHud();
        applyRadarHud();
        applySimpleElement(HUD_EAT, eat_layout);
        applySimpleElement(HUD_TAB, hud_button_star);
        applyChatHud();
    }
    public void applyHudElement(int id, float x, float y, float scalePercent) {
        if (id < 0 || id >= HUD_ELEMENTS) return;
        hudPosX[id] = x;
        hudPosY[id] = y;
        if (scalePercent > 0f) hudScaleX[id] = scalePercent;
        switch (id) {
            case HUD_HP:
                applyHealthHud();
                break;
            case HUD_HP_TEXT:
                // Deprecated: merged into HUD_HP
                break;
            case HUD_ARMOR:
                applyArmorHud();
                break;
            case HUD_ARMOR_TEXT:
                // Deprecated: merged into HUD_ARMOR
                break;
            case HUD_MONEY:
                if (!isClassicHud()) {
                    applySimpleElement(HUD_MONEY, hud_money);
                }
                break;
            case HUD_WANTED:
                if (!isClassicHud()) {
                    applyWantedHud();
                }
                break;
            case HUD_FIST:
                applyWeaponHud();
                break;
            case HUD_AMMO:
                // Deprecated: merged into HUD_FIST
                break;
            case HUD_RADAR:
                applyRadarHud();
                break;
            case HUD_CHAT:
                applyChatHud();
                break;
            case HUD_EAT:
                applySimpleElement(HUD_EAT, eat_layout);
                break;
            case HUD_TAB:
                applySimpleElement(HUD_TAB, hud_button_star);
                break;
            case HUD_ENTER_CAR_BTN:
                break;
            case HUD_CAMERA_BTN:
                break;
            case HUD_SHOOT_BTN:
                try {
                    setNativeWidgetShoot(x, y, (scalePercent > 0f) ? scalePercent : 100f, hudSensitivity[id], hudEnabled[id]);
                } catch (UnsatisfiedLinkError ignored) {}
                break;
        }
    }
    public void applyChatHud() {
        float posX = isClassicHud() ? 0.17f : 0.015f;
        float posY = 0.025f;
        float widthPercent = (hudScaleX[HUD_CHAT] > 0f) ? (hudScaleX[HUD_CHAT] / 100f) : (isClassicHud() ? 0.55f : 0.45f);
        float fontSize = (hudSensitivity[HUD_CHAT] > 0f) ? hudSensitivity[HUD_CHAT] : 20f;

        if (widthPercent < 0.25f) widthPercent = 0.25f;
        if (widthPercent > 0.85f) widthPercent = 0.85f;
        if (fontSize < 12f) fontSize = 12f;
        if (fontSize > 36f) fontSize = 36f;

        try {
            setNativeChatConfig(posX, posY, widthPercent, 0, fontSize);
        } catch (Throwable ignored) {}
    }
    private void applyHealthTextHud() {
        if (health_text != null) resetTransform(health_text);
    }
    private void applyArmorTextHud() {
        if (armour_text != null) resetTransform(armour_text);
    }
    private void applySimpleElement(int id, View view) {
        if (view == null) return;
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        applyAbsolutePosition(view, hudPosX[id], hudPosY[id]);
        if (hudScaleX[id] > 0f) applyScale(view, hudScaleX[id]);
    }
    private void applyHealthHud() {
        if (isClassicHud()) return;
        if (health_text != null) resetTransform(health_text);
        View target = health_layout;
        if (target == null) return;
        resetTransform(target);
        if (hudPosX[HUD_HP] >= 0f || hudPosY[HUD_HP] >= 0f) {
            applyAbsolutePosition(target, hudPosX[HUD_HP], hudPosY[HUD_HP]);
        }
        if (hudScaleX[HUD_HP] > 0f) {
            applyScale(target, hudScaleX[HUD_HP]);
        }
    }
    private void applyArmorHud() {
        if (isClassicHud()) return;
        if (armour_text != null) resetTransform(armour_text);
        View target = armour_layout;
        if (target == null) return;
        resetTransform(target);
        if (hudPosX[HUD_ARMOR] >= 0f || hudPosY[HUD_ARMOR] >= 0f) {
            applyAbsolutePosition(target, hudPosX[HUD_ARMOR], hudPosY[HUD_ARMOR]);
        }
        if (hudScaleX[HUD_ARMOR] > 0f) {
            applyScale(target, hudScaleX[HUD_ARMOR]);
        }
    }
    private void applyWeaponHud() {
        if (isClassicHud()) return;
        if (hud_ammo != null) resetTransform(hud_ammo);
        if (select_weapon != null) resetTransform(select_weapon);
        View target = (weapon_layout != null) ? weapon_layout : select_weapon;
        if (target == null) return;
        resetTransform(target);
        if (hudPosX[HUD_FIST] >= 0f || hudPosY[HUD_FIST] >= 0f) {
            applyAbsolutePosition(target, hudPosX[HUD_FIST], hudPosY[HUD_FIST]);
        }
        if (hudScaleX[HUD_FIST] > 0f) {
            applyScale(target, hudScaleX[HUD_FIST]);
        }
    }



    private void applyWantedHud() {
        ImageView[] stars = {star_1, star_2, star_3, star_4, star_5};



        for (ImageView star : stars) resetTransform(star);



        if (hudPosX[HUD_WANTED] >= 0f || hudPosY[HUD_WANTED] >= 0f) {
            float tx = 0f;
            float ty = 0f;



            if (star_1 != null && main_hud != null) {
                int[] screen = getScreenSize();
                int[] rootLoc = new int[2];
                int[] starLoc = new int[2];
                main_hud.getLocationOnScreen(rootLoc);
                star_1.getLocationOnScreen(starLoc);



                float baseX = starLoc[0] - rootLoc[0];
                float baseY = starLoc[1] - rootLoc[1];



                if (hudPosX[HUD_WANTED] >= 0f)
                    tx = hudPosX[HUD_WANTED] / 1920f * screen[0] - baseX;
                if (hudPosY[HUD_WANTED] >= 0f)
                    ty = hudPosY[HUD_WANTED] / 1080f * screen[1] - baseY;
            }



            for (ImageView star : stars) {
                if (star != null) {
                    star.setTranslationX(tx);
                    star.setTranslationY(ty);
                }
            }
        }



        if (hudScaleX[HUD_WANTED] > 0f) {
            for (ImageView star : stars) applyScale(star, hudScaleX[HUD_WANTED]);
        }
    }



    private void applyRadarHud() {
        if (isClassicHud()) {
            setNativeHudRadar(50.0f, 70.0f, 45.0f, 45.0f);
            return;
        }
        View target = (radar_layout != null) ? radar_layout : radar_bg;
        applySimpleElement(HUD_RADAR, target);
        updateRadarToCircleFrame();
    }



    private void resetTransform(View view) {
        if (view == null) return;
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }



    private void applyScale(View view, float percent) {
        if (view == null) return;



        float scale = Math.max(0.2f, Math.min(4.5f, percent / 100f));
        view.setPivotX(0f);
        view.setPivotY(0f);
        view.setScaleX(scale);
        view.setScaleY(scale);
    }



    private void applyAbsolutePosition(View view, float designX, float designY) {
        if (view == null || main_hud == null) return;



        int[] screen = getScreenSize();
        int[] rootLoc = new int[2];
        int[] viewLoc = new int[2];
        main_hud.getLocationOnScreen(rootLoc);
        view.getLocationOnScreen(viewLoc);



        float baseX = (viewLoc[0] - rootLoc[0]) - view.getTranslationX();
        float baseY = (viewLoc[1] - rootLoc[1]) - view.getTranslationY();



        if (designX >= 0f) view.setTranslationX(designX / 1920f * screen[0] - baseX);
        if (designY >= 0f) view.setTranslationY(designY / 1080f * screen[1] - baseY);
    }



    public float getHudPositionX(int id) {
        if (id < 0 || id >= HUD_ELEMENTS) return 0f;
        if (id == HUD_SHOOT_BTN) return (hudPosX[id] >= 0f) ? hudPosX[id] : 1700f;
        if (id == HUD_CHAT) return (hudPosX[id] >= 0f) ? hudPosX[id] : 30f;
        if (hudPosX[id] >= 0f) return hudPosX[id];
        View view = getAnchorView(id);
        return getDesignPositionX(view);
    }
    public float getHudPositionY(int id) {
        if (id < 0 || id >= HUD_ELEMENTS) return 0f;
        if (id == HUD_SHOOT_BTN) return (hudPosY[id] >= 0f) ? hudPosY[id] : 600f;
        if (id == HUD_CHAT) return (hudPosY[id] >= 0f) ? hudPosY[id] : 27f;
        if (hudPosY[id] >= 0f) return hudPosY[id];
        View view = getAnchorView(id);
        return getDesignPositionY(view);
    }
    public float getHudScale(int id) {
        if (id < 0 || id >= HUD_ELEMENTS) return 100f;
        if (id == HUD_CHAT) return (hudScaleX[id] > 0f) ? hudScaleX[id] : 45f;
        return (hudScaleX[id] > 0f) ? hudScaleX[id] : 100f;
    }
    private View getAnchorView(int id) {
        if (isClassicHud()) {
            return null;
        }
        switch (id) {
            case HUD_RADAR:
                return (radar_layout != null) ? radar_layout : radar_bg;
            case HUD_HP:
            case HUD_HP_TEXT:
                return health_layout;
            case HUD_ARMOR:
            case HUD_ARMOR_TEXT:
                return armour_layout;
            case HUD_MONEY:
                return hud_money;
            case HUD_WANTED:
                return star_1;
            case HUD_FIST:
            case HUD_AMMO:
                return (weapon_layout != null) ? weapon_layout : select_weapon;
            case HUD_EAT:
                return eat_layout;
            case HUD_TAB:
                return hud_button_star;
            default:
                return null;
        }
    }



    private float getDesignPositionX(View view) {
        if (view == null || main_hud == null) return 0f;



        int[] screen = getScreenSize();
        int[] rootLoc = new int[2];
        int[] viewLoc = new int[2];
        main_hud.getLocationOnScreen(rootLoc);
        view.getLocationOnScreen(viewLoc);



        return (viewLoc[0] - rootLoc[0]) / (float) screen[0] * 1920f;
    }



    private float getDesignPositionY(View view) {
        if (view == null || main_hud == null) return 0f;



        int[] screen = getScreenSize();
        int[] rootLoc = new int[2];
        int[] viewLoc = new int[2];
        main_hud.getLocationOnScreen(rootLoc);
        view.getLocationOnScreen(viewLoc);



        return (viewLoc[1] - rootLoc[1]) / (float) screen[1] * 1080f;
    }



    public void updatePlayerInfo(String name, int id) {
        this.playerName = name;
        this.playerId = id;
        updateLogoInfo();
    }



    public void UpdateHudInfo(int health, int armor, int weaponid, int ammo, int ammoinclip,
                              int money, int wanted, int eat, int drink, int bankMoney, int playerid) {
        if (isClassicHud()) {
            if (main_hud != null && main_hud.getVisibility() != View.GONE) {
                main_hud.setVisibility(View.GONE);
            }
            return;
        }
        if (health_text != null) {
            health_text.setText(String.format(Locale.US, "%d%%", health));
        }



        if (armour_text != null) {
            armour_text.setText(String.format(Locale.US, "%d%%", armor));
        }



        if (eat_text != null) {
            eat_text.setText("100%");
        }



        if (hud_money != null) {
            DecimalFormat formatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
            String sMoney = formatter.format(money);
            hud_money.setText(String.format(Locale.US, "$ %s", sMoney));
        }



        currentWeaponId = weaponid;
        currentAmmo = ammo;
        currentAmmoInClip = ammoinclip;
        updateWeaponAndAmmo(weaponid, ammo, ammoinclip);



        updateWantedStars(wanted);



        if (playerid != this.playerId) {
            this.playerId = playerid;
            updateLogoInfo();
        }

    }



    private static boolean s_hudWeaponBreadcrumbLogged = false;

    private void updateWeaponAndAmmo(int weaponid, int ammo, int ammoinclip) {
        if (!s_hudWeaponBreadcrumbLogged) {
            s_hudWeaponBreadcrumbLogged = true;
            try {
                File logFile = new File("/storage/emulated/0/GloryMobile/diag_log.txt");
                if (logFile.exists()) {
                    try (FileWriter fw = new FileWriter(logFile, true)) {
                        fw.write("[DIAG] First HUD weapon icon lookup (weaponid=" + weaponid + ")\n");
                    }
                }
            } catch (Exception ignored) {}
        }

        if (weapon_button != null) {
            try {
                String drawableName = "weapon_" + weaponid;
                int resId = activity.getResources().getIdentifier(
                        drawableName,
                        "drawable",
                        activity.getPackageName());

                if (resId != 0) {
                    weapon_button.setImageResource(resId);
                    weapon_button.setVisibility(View.VISIBLE);
                } else {
                    // Weapon id 0 (fists) or missing icon: hide cleanly without spamming logcat
                    weapon_button.setImageDrawable(null);
                    weapon_button.setVisibility(View.INVISIBLE);
                }
            } catch (Exception e) {
                weapon_button.setImageDrawable(null);
                weapon_button.setVisibility(View.INVISIBLE);
            }
        }



        if (weaponid == 0) {
            if (hud_ammo != null) {
                hud_ammo.setVisibility(View.INVISIBLE);
            }
        } else {
            if (hud_ammo != null) {
                hud_ammo.setVisibility(View.VISIBLE);
                hud_ammo.setText(String.format(Locale.US, "%d/%d", ammoinclip, ammo));
            }
        }
    }



    public void updateAmmoOnFire() {
        if (currentWeaponId != 0 && currentAmmoInClip > 0) {
            currentAmmoInClip--;
            if (hud_ammo != null && hud_ammo.getVisibility() == View.VISIBLE) {
                hud_ammo.setText(String.format(Locale.US, "%d/%d", currentAmmoInClip, currentAmmo));
            }
        }
    }



    public void updateAmmoOnReload(int newAmmoInClip, int totalAmmo) {
        currentAmmoInClip = newAmmoInClip;
        currentAmmo = totalAmmo;
        if (currentWeaponId != 0 && hud_ammo != null && hud_ammo.getVisibility() == View.VISIBLE) {
            hud_ammo.setText(String.format(Locale.US, "%d/%d", currentAmmoInClip, currentAmmo));
        }
    }



    private void updateLogoInfo() {
        if (hud_logo_text != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US);
            String currentDate = sdf.format(new Date());



            String infoText = String.format(Locale.US,
                    "ID %d: %s\n%s",
                    playerId,
                    playerName,
                    currentDate);



            hud_logo_text.setText(infoText);
        }
    }



    private void updateWantedStars(int wantedLevel) {



        if (wantedLevel <= 0) {
            setWantedStarState(star_1, false);
            setWantedStarState(star_2, false);
            setWantedStarState(star_3, false);
            setWantedStarState(star_4, false);
            setWantedStarState(star_5, false);



            if (star_1 != null) star_1.setVisibility(View.VISIBLE);
            if (star_2 != null) star_2.setVisibility(View.VISIBLE);
            if (star_3 != null) star_3.setVisibility(View.VISIBLE);
            if (star_4 != null) star_4.setVisibility(View.VISIBLE);
            if (star_5 != null) star_5.setVisibility(View.VISIBLE);
        } else {
            setWantedStarState(star_1, wantedLevel >= 1);
            setWantedStarState(star_2, wantedLevel >= 2);
            setWantedStarState(star_3, wantedLevel >= 3);
            setWantedStarState(star_4, wantedLevel >= 4);
            setWantedStarState(star_5, wantedLevel >= 5);



            if (star_1 != null) star_1.setVisibility(View.VISIBLE);
            if (star_2 != null) star_2.setVisibility(View.VISIBLE);
            if (star_3 != null) star_3.setVisibility(View.VISIBLE);
            if (star_4 != null) star_4.setVisibility(View.VISIBLE);
            if (star_5 != null) star_5.setVisibility(View.VISIBLE);
        }
    }



    private void setWantedStarState(ImageView star, boolean active) {
        if (star != null) {
            if (active) {
                star.setImageResource(R.drawable.ic_starnew_off);
            } else {
                star.setImageResource(R.drawable.ic_starnew_on);
            }
        }
    }



    public void toggleWeaponView() {
        if (select_weapon != null) {
            if (select_weapon.getVisibility() == View.VISIBLE) {
                select_weapon.setVisibility(View.INVISIBLE);
            } else {
                select_weapon.setVisibility(View.VISIBLE);
            }
        }
    }



    public void ShowHud() {
        if (main_hud != null) {
            boolean classic = isClassicHud();
            try {
                setNativeHudMode(classic);
            } catch (Throwable ignored) {}

            if (classic) {
                main_hud.setVisibility(View.GONE);
                setNativeHudRadar(50.0f, 70.0f, 45.0f, 45.0f);
                setNativeChatConfig(0.17f, 0.025f, 0.55f, 8, 20.0f);
            } else {
                main_hud.setVisibility(View.VISIBLE);
                updateRadarToCircleFrame();
                setNativeChatConfig(0.015f, 0.025f, 0.45f, 8, 20.0f);
                applyAllHudElements();
            }
            Log.d("HUD", "HUD shown (isClassic=" + classic + ")");

            if (isFirstShow) {
                isFirstShow = false;
                sendHudShownBroadcast();
            }
        }
    }



    public void HideHud() {
        if (main_hud != null) {
            main_hud.setVisibility(View.GONE);
            Log.d("HUD", "HUD hidden");
        }
    }



    private void sendHudShownBroadcast() {
        try {
            Intent hudShownIntent = new Intent("HUD_SHOWN");
            activity.sendBroadcast(hudShownIntent);
            Log.d("HUD", "Broadcast sent: HUD_SHOWN");
        } catch (Exception e) {
            Log.e("HUD", "Error sending broadcast: " + e.getMessage());
        }
    }



    public void ShowVehButton() {
        mIsVehicleMode = true;
    }

    public void HideVehButton() {
        mIsVehicleMode = false;
    }

    public void ShowVehButtonG() {}

    public void HideVehButtonG() {}

    public void updateRadarVisibility(boolean visible) {
        if (radar_zone != null) {
            radar_zone.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (radar_layout != null) {
            radar_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setWeaponVisibility(boolean visible) {
        if (weapon_button != null) {
            weapon_button.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (select_weapon != null) {
            select_weapon.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (hud_ammo != null) {
            hud_ammo.setVisibility(visible && currentWeaponId != 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setHealthVisibility(boolean visible) {
        if (health_layout != null) {
            health_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setArmourVisibility(boolean visible) {
        if (armour_layout != null) {
            armour_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setEatVisibility(boolean visible) {
        if (eat_layout != null) {
            eat_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setMoneyVisibility(boolean visible) {
        if (hud_money != null) {
            hud_money.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setButtonsVisibility(boolean visible) {
        if (samp_buttons_layout != null) {
            samp_buttons_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setLogoVisibility(boolean visible) {
        if (hud_logo_layout != null) {
            hud_logo_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }



    public void setMenuButtonClickListener(View.OnClickListener listener) {
        if (hud_button_menu != null) {
            hud_button_menu.setOnClickListener(listener);
        }
    }



    public void setStarButtonClickListener(View.OnClickListener listener) {
        if (hud_button_star != null) {
            hud_button_star.setOnClickListener(listener);
        }
    }



    public void setInvButtonClickListener(View.OnClickListener listener) {
        if (hud_button_inv != null) {
            hud_button_inv.setOnClickListener(listener);
        }
    }



    public void setShopButtonClickListener(View.OnClickListener listener) {
        if (hud_button_shop != null) {
            hud_button_shop.setOnClickListener(listener);
        }
    }



    public void setHelpButtonClickListener(View.OnClickListener listener) {
        if (hud_button_help != null) {
            hud_button_help.setOnClickListener(listener);
        }
    }
}

