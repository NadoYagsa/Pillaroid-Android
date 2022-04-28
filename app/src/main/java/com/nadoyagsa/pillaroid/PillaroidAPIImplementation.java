package com.nadoyagsa.pillaroid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class PillaroidAPIImplementation {
    private static final String BASE_URL = "http://ec2-13-209-8-46.ap-northeast-2.compute.amazonaws.com:8080/";

    public static PillaroidAPI getApiService() {
        return getInstance().create(PillaroidAPI.class);
    }

    private static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
