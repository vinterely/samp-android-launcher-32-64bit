package com.glory.launcher.activities;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.glory.launcher.Preferences;
import com.glory.game.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout cardHudGlory, cardHudClassic;
    private RadioButton radioHudGlory, radioHudClassic;

    private Button btnFps30, btnFps60, btnFps90, btnFps120;
    private SwitchCompat switchFpsCounter, switchFastConnect, switchVoiceChat;

    private SeekBar seekbarChatFont, seekbarChatWidth, seekbarChatLines;
    private TextView tvChatFontLabel, tvChatWidthLabel, tvChatLinesLabel;

    private Button btnSave, btnReset;

    private String selectedHud = "glory";
    private int selectedFps = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_settings_back);

        cardHudGlory = findViewById(R.id.card_hud_glory);
        cardHudClassic = findViewById(R.id.card_hud_classic);
        radioHudGlory = findViewById(R.id.radio_hud_glory);
        radioHudClassic = findViewById(R.id.radio_hud_classic);

        btnFps30 = findViewById(R.id.btn_fps_30);
        btnFps60 = findViewById(R.id.btn_fps_60);
        btnFps90 = findViewById(R.id.btn_fps_90);
        btnFps120 = findViewById(R.id.btn_fps_120);

        switchFpsCounter = findViewById(R.id.switch_fps_counter);
        switchFastConnect = findViewById(R.id.switch_fast_connect);
        switchVoiceChat = findViewById(R.id.switch_voice_chat);

        seekbarChatFont = findViewById(R.id.seekbar_chat_font);
        seekbarChatWidth = findViewById(R.id.seekbar_chat_width);
        seekbarChatLines = findViewById(R.id.seekbar_chat_lines);

        tvChatFontLabel = findViewById(R.id.tv_chat_font_label);
        tvChatWidthLabel = findViewById(R.id.tv_chat_width_label);
        tvChatLinesLabel = findViewById(R.id.tv_chat_lines_label);

        btnSave = findViewById(R.id.btn_save_settings);
        btnReset = findViewById(R.id.btn_reset_settings);
    }

    private void loadSettings() {
        // 1. HUD Type
        selectedHud = Preferences.getString(this, Preferences.HUD_TYPE, "glory");
        updateHudSelectionUI("classic".equalsIgnoreCase(selectedHud));

        // 2. FPS Limit
        String fpsStr = Preferences.getString(this, Preferences.FPS_LIMIT, "60");
        try {
            selectedFps = Integer.parseInt(fpsStr);
        } catch (Exception e) {
            selectedFps = 60;
        }
        updateFpsUI(selectedFps);

        // 3. FPS Counter & Fast Connect
        boolean fpsCounter = Preferences.getBoolean(this, Preferences.FPS_COUNTER, false);
        switchFpsCounter.setChecked(fpsCounter);

        boolean fastConnect = Preferences.getBoolean(this, Preferences.FAST_CONNECT, true);
        switchFastConnect.setChecked(fastConnect);

        boolean voiceChat = Preferences.getBoolean(this, Preferences.VOICECHAT, true);
        switchVoiceChat.setChecked(voiceChat);

        // 4. Chat settings
        int chatFont = 20;
        try {
            String fontStr = Preferences.getString(this, Preferences.CHAT_FONT_SIZE, "20");
            chatFont = Math.round(Float.parseFloat(fontStr));
        } catch (Exception ignored) {}
        if (chatFont < 14) chatFont = 14;
        if (chatFont > 36) chatFont = 36;
        seekbarChatFont.setProgress(chatFont - 14);
        tvChatFontLabel.setText(String.format(Locale.getDefault(), "اندازه قلم چت: %dpt", chatFont));

        int chatWidth = 45;
        try {
            String widthStr = Preferences.getString(this, Preferences.CHAT_WIDTH_PERCENT, "45");
            chatWidth = Math.round(Float.parseFloat(widthStr));
        } catch (Exception ignored) {}
        if (chatWidth < 30) chatWidth = 30;
        if (chatWidth > 70) chatWidth = 70;
        seekbarChatWidth.setProgress(chatWidth - 30);
        tvChatWidthLabel.setText(String.format(Locale.getDefault(), "عرض کادر چت: %d%%", chatWidth));

        int chatLines = 7;
        try {
            String linesStr = Preferences.getString(this, Preferences.CHAT_LINES_COUNT, "7");
            chatLines = Integer.parseInt(linesStr);
        } catch (Exception ignored) {}
        if (chatLines < 4) chatLines = 4;
        if (chatLines > 15) chatLines = 15;
        seekbarChatLines.setProgress(chatLines - 4);
        tvChatLinesLabel.setText(String.format(Locale.getDefault(), "تعداد خطوط چت: %d خط", chatLines));
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistSettings(false);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                persistSettings(false);
                finish();
            });
        }

        // HUD Selection
        if (cardHudGlory != null) {
            cardHudGlory.setOnClickListener(v -> {
                selectedHud = "glory";
                updateHudSelectionUI(false);
                persistSettings(false);
            });
        }
        if (cardHudClassic != null) {
            cardHudClassic.setOnClickListener(v -> {
                selectedHud = "classic";
                updateHudSelectionUI(true);
                persistSettings(false);
            });
        }

        // FPS Limit Buttons
        btnFps30.setOnClickListener(v -> { selectedFps = 30; updateFpsUI(30); });
        btnFps60.setOnClickListener(v -> { selectedFps = 60; updateFpsUI(60); });
        btnFps90.setOnClickListener(v -> { selectedFps = 90; updateFpsUI(90); });
        btnFps120.setOnClickListener(v -> { selectedFps = 120; updateFpsUI(120); });

        // SeekBars
        seekbarChatFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int font = 14 + progress;
                tvChatFontLabel.setText(String.format(Locale.getDefault(), "اندازه قلم چت: %dpt", font));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekbarChatWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int width = 30 + progress;
                tvChatWidthLabel.setText(String.format(Locale.getDefault(), "عرض کادر چت: %d%%", width));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekbarChatLines.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int lines = 4 + progress;
                tvChatLinesLabel.setText(String.format(Locale.getDefault(), "تعداد خطوط چت: %d خط", lines));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save Button
        btnSave.setOnClickListener(v -> saveSettings());

        // Reset Button
        btnReset.setOnClickListener(v -> resetToDefaults());
    }

    private void updateHudSelectionUI(boolean isClassic) {
        if (isClassic) {
            cardHudClassic.setBackgroundResource(R.drawable.settings_card_selected_bg);
            radioHudClassic.setChecked(true);
            cardHudGlory.setBackgroundResource(R.drawable.settings_card_bg);
            radioHudGlory.setChecked(false);
        } else {
            cardHudGlory.setBackgroundResource(R.drawable.settings_card_selected_bg);
            radioHudGlory.setChecked(true);
            cardHudClassic.setBackgroundResource(R.drawable.settings_card_bg);
            radioHudClassic.setChecked(false);
        }
    }

    private void updateFpsUI(int fps) {
        btnFps30.setBackgroundResource(fps == 30 ? R.drawable.settings_pill_selected_bg : R.drawable.settings_pill_bg);
        btnFps30.setTextColor(fps == 30 ? 0xFF101014 : 0xFFFFFFFF);

        btnFps60.setBackgroundResource(fps == 60 ? R.drawable.settings_pill_selected_bg : R.drawable.settings_pill_bg);
        btnFps60.setTextColor(fps == 60 ? 0xFF101014 : 0xFFFFFFFF);

        btnFps90.setBackgroundResource(fps == 90 ? R.drawable.settings_pill_selected_bg : R.drawable.settings_pill_bg);
        btnFps90.setTextColor(fps == 90 ? 0xFF101014 : 0xFFFFFFFF);

        btnFps120.setBackgroundResource(fps == 120 ? R.drawable.settings_pill_selected_bg : R.drawable.settings_pill_bg);
        btnFps120.setTextColor(fps == 120 ? 0xFF101014 : 0xFFFFFFFF);
    }

    private void persistSettings(boolean showToast) {
        // Save to Preferences
        Preferences.putString(this, Preferences.HUD_TYPE, selectedHud);
        Preferences.putString(this, Preferences.FPS_LIMIT, String.valueOf(selectedFps));
        if (switchFpsCounter != null) Preferences.putBoolean(this, Preferences.FPS_COUNTER, switchFpsCounter.isChecked());
        if (switchFastConnect != null) Preferences.putBoolean(this, Preferences.FAST_CONNECT, switchFastConnect.isChecked());
        if (switchVoiceChat != null) Preferences.putBoolean(this, Preferences.VOICECHAT, switchVoiceChat.isChecked());

        int chatFont = (seekbarChatFont != null) ? (14 + seekbarChatFont.getProgress()) : 20;
        int chatWidth = (seekbarChatWidth != null) ? (30 + seekbarChatWidth.getProgress()) : 45;
        int chatLines = (seekbarChatLines != null) ? (4 + seekbarChatLines.getProgress()) : 7;

        Preferences.putString(this, Preferences.CHAT_FONT_SIZE, String.valueOf(chatFont));
        Preferences.putString(this, Preferences.CHAT_WIDTH_PERCENT, String.valueOf(chatWidth));
        Preferences.putString(this, Preferences.CHAT_LINES_COUNT, String.valueOf(chatLines));

        // Sync to settings.ini if available
        syncToSettingsIni(selectedFps, switchFpsCounter != null && switchFpsCounter.isChecked(), chatLines, chatFont, chatWidth);

        if (showToast) {
            Toast.makeText(this, "تنظیمات با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSettings() {
        persistSettings(true);
        finish();
    }

    private void resetToDefaults() {
        selectedHud = "glory";
        updateHudSelectionUI(false);

        selectedFps = 60;
        updateFpsUI(60);

        if (switchFpsCounter != null) switchFpsCounter.setChecked(false);
        if (switchFastConnect != null) switchFastConnect.setChecked(true);
        if (switchVoiceChat != null) switchVoiceChat.setChecked(true);

        if (seekbarChatFont != null) seekbarChatFont.setProgress(6); // 20
        if (seekbarChatWidth != null) seekbarChatWidth.setProgress(15); // 45
        if (seekbarChatLines != null) seekbarChatLines.setProgress(3); // 7

        tvChatFontLabel.setText("اندازه قلم چت: 20pt");
        tvChatWidthLabel.setText("عرض کادر چت: 45%");
        tvChatLinesLabel.setText("تعداد خطوط چت: 7 خط");

        persistSettings(false);
        Toast.makeText(this, "تنظیمات به حالت پیش‌فرض بازگردانی شد", Toast.LENGTH_SHORT).show();
    }

    private void syncToSettingsIni(int fpsLimit, boolean fpsCounter, int chatLines, int chatFontSize, int chatWidth) {
        try {
            File externalDir = getExternalFilesDir(null);
            File iniFile = null;
            if (externalDir != null) {
                iniFile = new File(externalDir, "SAMP/settings.ini");
            }
            if (iniFile == null || !iniFile.exists()) {
                File altFile = new File(Environment.getExternalStorageDirectory(), "Android/data/com.glory.game/files/SAMP/settings.ini");
                if (altFile.exists()) {
                    iniFile = altFile;
                }
            }

            if (iniFile != null && iniFile.exists()) {
                List<String> lines = new ArrayList<>();
                boolean foundHudType = false;
                boolean isClassic = "classic".equalsIgnoreCase(selectedHud);
                try (BufferedReader reader = new BufferedReader(new FileReader(iniFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("FPSLimit=")) {
                            lines.add("FPSLimit=" + fpsLimit);
                        } else if (trimmed.startsWith("fps=")) {
                            lines.add("fps=" + (fpsCounter ? "1" : "0"));
                        } else if (trimmed.startsWith("chatlines=")) {
                            lines.add("chatlines=" + chatLines);
                        } else if (trimmed.startsWith("hud_type=")) {
                            lines.add("hud_type=" + selectedHud);
                            foundHudType = true;
                        } else if (trimmed.startsWith("ChatPosX=") && isClassic) {
                            lines.add("ChatPosX=325.000000");
                        } else if (trimmed.startsWith("ChatPosY=") && isClassic) {
                            lines.add("ChatPosY=25.000000");
                        } else if (trimmed.startsWith("ChatSizeX=") && isClassic) {
                            lines.add("ChatSizeX=1150.000000");
                        } else {
                            lines.add(line);
                        }
                    }
                }

                if (!foundHudType) {
                    lines.add("hud_type=" + selectedHud);
                }

                try (FileWriter writer = new FileWriter(iniFile, false)) {
                    for (String l : lines) {
                        writer.write(l + "\n");
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
