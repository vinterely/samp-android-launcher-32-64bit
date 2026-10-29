package com.glory.launcher.util;

import java.util.Locale;

public class PersianUtils {

    private static final char[] PERSIAN_DIGITS = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};

    public static String toPersianDigits(int number) {
        return toPersianDigits(String.valueOf(number));
    }

    public static String toPersianDigits(long number) {
        return toPersianDigits(String.valueOf(number));
    }

    public static String toPersianDigits(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(PERSIAN_DIGITS[ch - '0']);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return toPersianDigits("0") + " بایت";
        final String[] units = new String[]{"بایت", "کیلوبایت", "مگابایت", "گیگابایت", "ترابایت"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        double value = bytes / Math.pow(1024, digitGroups);
        String formatted = String.format(Locale.US, digitGroups == 0 ? "%.0f" : "%.1f", value);
        return toPersianDigits(formatted) + " " + units[digitGroups];
    }

    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec <= 0) return toPersianDigits("0") + " KB/s";
        double kb = bytesPerSec / 1024.0;
        if (kb < 1024.0) {
            String val = String.format(Locale.US, "%.1f", kb);
            return toPersianDigits(val) + " KB/s";
        }
        double mb = kb / 1024.0;
        String val = String.format(Locale.US, "%.1f", mb);
        return toPersianDigits(val) + " MB/s";
    }

    public static String formatDuration(long seconds) {
        if (seconds < 0) seconds = 0;
        long mins = seconds / 60;
        long secs = seconds % 60;
        String formatted = String.format(Locale.US, "%02d:%02d", mins, secs);
        return toPersianDigits(formatted);
    }
}
