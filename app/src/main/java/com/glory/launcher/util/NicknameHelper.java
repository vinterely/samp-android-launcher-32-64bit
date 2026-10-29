package com.glory.launcher.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glory.Config;
import com.glory.Utils;
import com.glory.game.R;
import com.glory.launcher.Preferences;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

import es.dmoral.toasty.Toasty;

/**
 * Helper class for player nickname management (validation, saving to SharedPreferences and settings.ini, and dialog editing).
 */
public class NicknameHelper {

    public interface OnNicknameSavedListener {
        void onNicknameSaved(String newNickname);
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Get currently saved nickname.
     */
    public static String getNickname(Context context) {
        if (context == null) return "";
        return Preferences.getString(context, Preferences.NICKNAME, "");
    }

    /**
     * Validate nickname according to SA-MP standard rules:
     * - Must not be empty
     * - Must contain an underscore '_'
     * - Must have at least 4 characters
     * - Must have at most 20 characters
     */
    public static ValidationResult validateNickname(Context context, String nickname) {
        if (context == null) {
            return new ValidationResult(false, "Invalid context");
        }

        if (TextUtils.isEmpty(nickname) || nickname.trim().isEmpty()) {
            return new ValidationResult(false, context.getString(R.string.enterNik));
        }

        String trimmed = nickname.trim();

        if (trimmed.length() < 4) {
            return new ValidationResult(false, context.getString(R.string.minLengthNik));
        }

        if (!trimmed.contains("_")) {
            return new ValidationResult(false, context.getString(R.string.mustContains_));
        }

        if (trimmed.length() > 24) {
            return new ValidationResult(false, "طول نام کاربری نباید بیشتر از ۲۴ نویسه باشد");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Save nickname to SharedPreferences and settings.ini.
     */
    public static boolean saveNickname(Context context, String rawNickname) {
        if (context == null || rawNickname == null) return false;
        String nickname = rawNickname.trim();

        // 1. Save to SharedPreferences
        Preferences.putString(context, Preferences.NICKNAME, nickname);

        // 2. Save to SAMP/settings.ini
        try {
            File settingsFile = new File(Config.GAME_PATH + "SAMP/settings.ini");
            if (settingsFile.exists()) {
                Wini wini = new Wini(settingsFile);
                wini.put("client", "name", nickname);
                wini.store();
                Utils.writeLog(context, 'i', "Nickname saved to settings.ini: " + nickname);
            } else {
                // If directory exists, create file
                File sampDir = settingsFile.getParentFile();
                if (sampDir != null && sampDir.exists()) {
                    settingsFile.createNewFile();
                    Wini wini = new Wini(settingsFile);
                    wini.put("client", "name", nickname);
                    wini.store();
                } else {
                    Utils.writeLog(context, 'w', "SAMP dir not found for settings.ini");
                }
            }
            return true;
        } catch (IOException e) {
            Utils.writeLog(context, 'e', "Failed to write nickname to settings.ini: " + e.getMessage());
            return false;
        }
    }

    /**
     * Show a clean dialog for editing the nickname.
     */
    public static void showEditNicknameDialog(Activity activity, OnNicknameSavedListener listener) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 40, 48, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundResource(R.drawable.card_translucent_bg);

        TextView title = new TextView(activity);
        title.setText("تغییر نام کاربری");
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("نام خود را با فرمت Name_Family وارد کنید");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.parseColor("#B3B0C8"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 12, 0, 24);
        layout.addView(subtitle);

        EditText input = new EditText(activity);
        input.setText(getNickname(activity));
        input.setHint("مثال: Ali_Reza");
        input.setHintTextColor(Color.parseColor("#7A7395"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(15);
        input.setBackgroundResource(R.drawable.edit_text_bg);
        input.setPadding(32, 24, 32, 24);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER);
        layout.addView(input);

        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setPadding(0, 32, 0, 0);

        Button btnCancel = new Button(activity);
        btnCancel.setText("انصراف");
        btnCancel.setTextColor(Color.parseColor("#B3B0C8"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnLayout.addView(btnCancel);

        Button btnSave = new Button(activity);
        btnSave.setText("ذخیره");
        btnSave.setTextColor(Color.parseColor("#14091f"));
        btnSave.setBackgroundResource(R.drawable.btn_amber_selector);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        saveParams.setMarginStart(24);
        btnSave.setLayoutParams(saveParams);

        btnSave.setOnClickListener(v -> {
            String newNick = input.getText().toString().trim();
            ValidationResult result = validateNickname(activity, newNick);
            if (!result.isValid) {
                Toasty.warning(activity, result.errorMessage, Toast.LENGTH_SHORT).show();
                return;
            }

            saveNickname(activity, newNick);
            Toasty.success(activity, "نام کاربری با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            if (listener != null) {
                listener.onNicknameSaved(newNick);
            }
        });

        btnLayout.addView(btnSave);
        layout.addView(btnLayout);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }
}
