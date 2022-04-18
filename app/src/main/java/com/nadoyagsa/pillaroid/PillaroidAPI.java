package com.nadoyagsa.pillaroid;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface PillaroidAPI {
    @POST("/login/kakao")
    Call<String> postLogin(@Body JsonObject kakaoToken);    //서버로 카카오 토큰 전달(일반로그인)
}
