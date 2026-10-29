package com.glory.launcher.network;

import com.google.gson.annotations.SerializedName;

public final class Story {
    @SerializedName("image")
    private String image;
    @SerializedName("link")
    private String link;
    @SerializedName("text")
    private String text;
    @SerializedName("title")
    private String title;

    public Story(String str, String str2, String str3, String str4) {
        this.title = str;
        this.text = str2;
        this.image = str3;
        this.link = str4;
    }

    public final String getTitle() { return this.title; }

    public final String getText() { return this.text; }

    public final String getImage() { return this.image; }

    public final String getLink() { return this.link; }
}
