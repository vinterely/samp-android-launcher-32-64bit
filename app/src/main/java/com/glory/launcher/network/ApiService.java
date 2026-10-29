package com.glory.launcher.network;

import com.glory.Config;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {
    private static ApiService instance;
    private Retrofit retrofit = new Retrofit.Builder()./*client(new OkHttpClient.Builder().
            addInterceptor(new UserAgentInterceptor("")).build()).*/
            baseUrl(Config.API_LINK).addConverterFactory(GsonConverterFactory.create()).build();

    private ApiService() {
    }

    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public Api getApiService() {
        return this.retrofit.create(Api.class);
    }
}
