package com.glory.launcher.network;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Api {

    @GET("/glory/api.json")
    Call<Links> getLinks();

    @GET("/glory/servers.json")
    Call<ArrayList<Server>> getServers();

    @GET("/glory/news.json")
    Call<ArrayList<Story>> getStories();
}

