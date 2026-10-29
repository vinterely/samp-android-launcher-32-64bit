package com.glory.launcher.activities;

public class UnZip {
    public static String getExtractCmd(String str, String str2) {
        return String.format(java.util.Locale.US, "7z x '%s' '-o%s' -aoa", new Object[]{ str, str2 });
    }

    public static String getCompressCmd(String str, String str2, String str3) {
        return String.format(java.util.Locale.US, "7z a -t%s '%s' '%s'", new Object[]{ str3, str2, str });
    }
}
