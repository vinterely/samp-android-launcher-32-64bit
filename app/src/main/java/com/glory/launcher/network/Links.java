package com.glory.launcher.network;

import com.glory.game.BuildConfig;
import com.google.gson.annotations.SerializedName;

public class Links {

    // Global Project URLs & Config
    public static final String MANIFEST_URL = "https://example.com/launcher/manifest.json";
    public static final String TELEGRAM_CHANNEL = "https://t.me/YourChannel";
    public static final String TELEGRAM_GROUP = "https://t.me/YourChannel";
    public static final String TELEGRAM_SUPPORT = "https://t.me/YourChannel";
    public static final String TELEGRAM_TUTORIAL = "https://t.me/YourChannel";
    public static final String TELEGRAM_SHOP = "https://t.me/YourChannel";
    public static final String APARAT = "https://www.aparat.com/YourChannel";
    public static final String SOROUSH = "https://splus.ir/YourChannel";
    public static final String SITE = "https://example.com";
    public static final String WEBSITE = SITE;
    public static final String FORUM = "https://forum.example.com";
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 7777;

    @SerializedName("URL_CLIENT")
    private String URL_CLIENT;
    @SerializedName("URL_GAME_FILES")
    private String URL_GAME_FILES;
    @SerializedName("URL_GAME_FILES_UPD")
    private String URL_GAME_FILES_UPDATE;
    @SerializedName("URL_VK")
    private String URL_VK;
    @SerializedName("URL_DISCORD")
    private String URL_DISCORD;
    @SerializedName("URL_YOUTUBE")
    private String URL_YOUTUBE;
    @SerializedName("URL_FORUM")
    private String URL_FORUM;
    @SerializedName("URL_DONATE")
    private String URL_DONATE;
    @SerializedName("clientVersionCode")
    private Integer targetClientVersion = BuildConfig.VERSION_CODE;
    @SerializedName("gameFilesVersionCode")
    private Integer targetGameFilesVersion;

    public Links() { }

    public final Integer getTargetClientVersion() {
        return targetClientVersion;
    }

    public final Integer getTargetGameFilesVersion() {
        return targetGameFilesVersion;
    }

    public final String getUrlClient() {
        return URL_CLIENT;
    }
    public final String getUrlFiles() {
        return URL_GAME_FILES;
    }
    public final String getUrlFilesUpdate() {
        return URL_GAME_FILES_UPDATE;
    }
    public final String getUrlForum() {
        return URL_FORUM;
    }
    public final String getUrlDonate() {
        return URL_DONATE;
    }
    public final String getUrlVKontakte() {
        return URL_VK;
    }
    public final String getUrlDiscord() {
        return URL_DISCORD;
    }
    public final String getUrlYoutube() {
        return URL_YOUTUBE;
    }
}
