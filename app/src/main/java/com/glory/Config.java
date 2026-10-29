package com.glory;

public class Config {
    public static String APP_PATH = "/storage/emulated/0/GloryMobile/";
    public static String PATH_DOWNLOADS = APP_PATH + "downloads/";
    public static String GAME_PATH = APP_PATH;
    public static final String API_LINK = "https://glory-rp.ir/";

    public static final String VK_LINK = "https://discord.gg/9t8wgbAzM8";
    public static final String YT_LINK = "https://discord.gg/9t8wgbAzM8";
    public static final String DS_LINK = "https://discord.gg/9t8wgbAzM8";

    public static void setGamePath(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        APP_PATH = path;
        GAME_PATH = path;
        PATH_DOWNLOADS = path + "downloads/";
    }
}
