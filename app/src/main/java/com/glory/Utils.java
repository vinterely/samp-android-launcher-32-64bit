package com.glory;

import static com.glory.Config.APP_PATH;
import static com.glory.Config.GAME_PATH;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import es.dmoral.toasty.Toasty;
import org.ini4j.Wini;
import android.content.Intent;
import com.glory.launcher.activities.DownloadActivity;
import com.glory.launcher.activities.ChooseServerActivity;

public class Utils extends AppCompatActivity {

    protected void onCreate(Bundle bundle) { super.onCreate(bundle); }

    static boolean downloading = false;
    static Integer typeInstall = 0;
    public static Integer INSTALL_TYPE_CLIENT = 1;
    public static Integer INSTALL_TYPE_REINSTALL = 2;
    public static Integer INSTALL_TYPE_UPDATE_GAMEFILES = 3;
    public static Integer INSTALL_TYPE_GRAPHICS = 4;

    // 1 - reinstall the game (only files)
    // 2 - graph (gta_sa.set)
    public static void handleServerPlay(
            Context context,
            Activity activity,
            int serverId
    ) {
        if (!Utils.isGameInstalled()) return;

        File gameFiles = new File(Config.GAME_PATH + "data/ver.ini");
        int TARGET_GAMEFILES_VERSION = 0;

        if (gameFiles.exists()) {
            try {
                Wini w = new Wini(gameFiles);
                TARGET_GAMEFILES_VERSION = Integer.parseInt(
                        w.get("versions", "gameFilesVersion")
                );
            } catch (Exception e) {
                Utils.writeLog(context, 'e', e.getMessage());
            }
        }

        if (App.getInstance().targetGameFilesVersion != null
                && App.getInstance().targetGameFilesVersion != TARGET_GAMEFILES_VERSION) {
            App.getInstance().downloadID = App.INSTALL_TYPE_UPDATE_GAMEFILES;
            context.startActivity(new Intent(context, DownloadActivity.class));
            activity.finish();
            return;
        }

        File settings = new File(Config.GAME_PATH + "SAMP/settings.ini");
        if (!settings.exists()) {
            prepareSampDir(Config.GAME_PATH);
        }
        if (settings.exists()) {
            try {
                Wini w = new Wini(settings);
                w.put("client", "server", serverId);
                String nick = com.glory.launcher.util.NicknameHelper.getNickname(context);
                if (nick != null && !nick.isEmpty()) {
                    w.put("client", "name", nick);
                }
                w.store();

                if (activity instanceof com.glory.launcher.activities.ChooseServerActivity) {
                    Intent gameIntent = new Intent(context, com.glory.game.SAMP.class);
                    context.startActivity(gameIntent);
                    activity.finish();
                } else {
                    context.startActivity(
                            new Intent(context, com.glory.launcher.activities.ChooseServerActivity.class)
                    );
                    activity.finish();
                }

            } catch (IOException e) {
                Utils.writeLog(context, 'e', e.getMessage());
            }
        }
    }


    public static boolean getDownloading() { return downloading; }
    public static Integer getInstallType () { return typeInstall; }
    public static boolean setDownloading(boolean value) { return downloading = value; }
    public static Integer setInstallType(int type) { return typeInstall = type; }

    public static void writeLog(Activity activity, char type, String message) {
        File logFile = new File(activity.getExternalFilesDir((String) null).getPath() + "/logs.txt");
        try {
            Date dateNow = new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.ENGLISH);
            if (logFile.exists()) {
                Writer output = new BufferedWriter(new FileWriter(logFile, true));
                if (type == 'e') {
                    Log.e("LOG", message);
                    output.write("\nERROR: ");
                } else if (type == 'i') {
                    Log.i("LOG", message);
                    output.write("\nINFO: ");
                } else if (type == 'w') {
                    Log.w("LOG", message);
                    output.write("\nWARNING: ");
                }
                output.write(formatForDateNow.format(dateNow) + " - " + message);
                output.flush();
                output.close();
            } else if (logFile.createNewFile()) {
                Writer output2 = new BufferedWriter(new FileWriter(logFile, false));
                if (type == 'e') {
                    output2.write("ERROR: ");
                } else if (type == 'i') {
                    output2.write("INFO: ");
                } else if (type == 'w') {
                    output2.write("WARNING: ");
                }
                output2.write(formatForDateNow.format(dateNow) + " - " + message);
                output2.flush();
                output2.close();
            }
        } catch (IOException e) {
            Log.e("LOG", e.toString());
        }
    }
    public static void writeLog(Context context, char type, String message) {
        File logFile = new File(context.getExternalFilesDir((String) null).getPath() + "/logs.txt");
        try {
            Date dateNow = new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.ENGLISH);
            if (logFile.exists()) {
                Writer output = new BufferedWriter(new FileWriter(logFile, true));
                if (type == 'e') {
                    output.write("\nERROR: ");
                } else if (type == 'i') {
                    output.write("\nINFO: ");
                } else if (type == 'w') {
                    output.write("\nWARNING: ");
                }
                output.write(formatForDateNow.format(dateNow) + " - " + message);
                output.flush();
                output.close();
            } else if (logFile.createNewFile()) {
                Writer output2 = new BufferedWriter(new FileWriter(logFile, false));
                if (type == 'e') {
                    output2.write("ERROR: ");
                } else if (type == 'i') {
                    output2.write("INFO: ");
                } else if (type == 'w') {
                    output2.write("WARNING: ");
                }
                output2.write(formatForDateNow.format(dateNow) + " - " + message);
                output2.flush();
                output2.close();
            }
        } catch (IOException e) {
            Log.e("LOG", e.toString());
        }
    }
    public static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next().replace(",", ",\n") : "";
    }
    public static String formatFileSize(long size) {
        String hrSize = null;

        double b = size;
        double k = size/1024.0;
        double m = ((size/1024.0)/1024.0);
        double g = (((size/1024.0)/1024.0)/1024.0);
        double t = ((((size/1024.0)/1024.0)/1024.0)/1024.0);

        DecimalFormat dec = new DecimalFormat("0.00", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));

        if ( t>1 ) {
            hrSize = dec.format(t).concat(" TB");
        } else if ( g>1 ) {
            hrSize = dec.format(g).concat(" GB");
        } else if ( m>1 ) {
            hrSize = dec.format(m).concat(" MB");
        } else if ( k>1 ) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" B");
        }
        return hrSize;
    }
    public static void showMessage(String _s, Context context) {
        Toasty.info(context, _s, Toast.LENGTH_LONG).show(); }

    public static boolean isInternetConnected(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) { return true; }
        return false;
    }

    public static boolean hasStoragePermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return androidx.core.content.ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean checkGameInPath(String path) {
        if (path == null || path.isEmpty()) return false;
        if (!path.endsWith("/")) path += "/";
        File anim = new File(path + "anim");
        File data = new File(path + "data");
        File models = new File(path + "models");
        return anim.exists() && data.exists() && models.exists();
    }

    public static boolean isGameInstalled() {
        // 1. Check current configured path
        if (checkGameInPath(Config.GAME_PATH)) {
            prepareSampDir(Config.GAME_PATH);
            return true;
        }

        // 2. Check candidate directories
        String extRoot = "";
        try {
            extRoot = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        } catch (Exception ignored) {}

        String[] candidates = new String[] {
            "/storage/emulated/0/GloryMobile/files/",
            "/storage/emulated/0/GloryMobile/",
            "/storage/emulated/0/GTA/files/",
            "/storage/emulated/0/GTA/",
            extRoot.isEmpty() ? "" : extRoot + "/GTA/files/",
            extRoot.isEmpty() ? "" : extRoot + "/GTA/",
            "/sdcard/GTA/files/",
            "/sdcard/GTA/",
            extRoot.isEmpty() ? "" : extRoot + "/Android/data/com.gloryrp.app/files/", extRoot + "/Android/data/com.glory.game/files/"
        };

        for (String candidate : candidates) {
            if (candidate.isEmpty()) continue;
            if (checkGameInPath(candidate)) {
                Config.setGamePath(candidate);
                prepareSampDir(candidate);
                return true;
            }
        }

        return false;
    }

    public static void prepareSampDir(String gamePath) {
        if (!gamePath.endsWith("/")) gamePath += "/";
        File sampDir = new File(gamePath + "SAMP");
        if (!sampDir.exists()) sampDir.mkdirs();
        File settings = new File(sampDir, "settings.ini");
        if (!settings.exists() || settings.length() == 0) {
            try {
                String nick = "Player";
                try {
                    nick = com.glory.launcher.Preferences.getString(com.glory.App.getInstance(), com.glory.launcher.Preferences.NICKNAME, "Player");
                } catch (Exception ignored) {}

                String iniContent = "[client]\n"
                        + "name=" + nick + "\n"
                        + "server=0\n"
                        + "host=play.glory-rp.ir\n"
                        + "port=7777\n"
                        + "version=0.3.7\n"
                        + "password=\n"
                        + "autoaim=0\n\n"
                        + "[gui]\n"
                        + "Font=arial.ttf\n"
                        + "FontSize=30.000000\n"
                        + "FontOutline=2\n"
                        + "ChatPosX=325.000000\n"
                        + "ChatPosY=25.000000\n"
                        + "ChatSizeX=1150.000000\n"
                        + "ChatSizeY=220.000000\n"
                        + "ChatMaxMessages=8\n"
                        + "SpawnScreenPosX=660.000000\n"
                        + "SpawnScreenPosY=950.000000\n"
                        + "SpawnScreenSizeX=600.000000\n"
                        + "SpawnScreenSizeY=100.000000\n"
                        + "HealthBarWidth=100.000000\n"
                        + "HealthBarHeight=10.000000\n"
                        + "ScoreBoardSizeX=846.000000\n"
                        + "ScoreBoardSizeY=614.000000\n"
                        + "Dialog=1\n"
                        + "fpsinfo=0\n"
                        + "FPSLimit=60\n"
                        + "firstperson=1\n";

                java.io.FileWriter fw = new java.io.FileWriter(settings);
                fw.write(iniContent);
                fw.flush();
                fw.close();
            } catch (Exception ignored) {}
        }
    }

    public static String getDiagnosisInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        boolean perm = hasStoragePermission(context);
        sb.append("دسترسی حافظه (Storage Permission): ").append(perm ? "تایید شده (OK)" : "رد شده (DENIED!)").append("\n\n");

        sb.append("بررسی پوشه‌ها:\n");
        String[] checks = new String[] {
            "/storage/emulated/0/GloryMobile/",
            "/storage/emulated/0/GloryMobile/files/",
            "/storage/emulated/0/GTA/",
            "/storage/emulated/0/GTA/files/"
        };
        for (String p : checks) {
            File f = new File(p);
            sb.append("• ").append(p).append(" -> ");
            if (!f.exists()) {
                sb.append("وجود ندارد (not found)\n");
            } else {
                sb.append("پوشه هست. محتویات: ");
                String[] list = f.list();
                if (list == null) {
                    sb.append("عدم دسترسی به خواندن!\n");
                } else {
                    sb.append(java.util.Arrays.toString(list)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static void writeLog(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String text = sw.toString();
        writeFile(APP_PATH+"/log.txt", text);
    }

    public static void writeFile(String path, String str)
    {
        File file = new File(path);
        try { if (!file.exists()) file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File(path), false);
            fileWriter.write(str);
            fileWriter.flush();
        } catch (IOException e) { e.printStackTrace(); } finally { try { if (fileWriter != null) fileWriter.close(); } catch (IOException e) { e.printStackTrace(); }}
    }
}


