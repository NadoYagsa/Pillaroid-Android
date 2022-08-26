package com.nadoyagsa.pillaroid;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PillaroidAPI {
    @POST("/login/kakao")
    Call<String> postLogin(@Body JsonObject kakaoToken);                                            // 서버로 카카오 토큰 전달(일반로그인)


    @GET("/medicine")
    Call<String> getMedicineByIdx(@Header("authorization") String jwt, @Query("idx") int medicineIdx);          // 의약품 idx로 의약품 정보 조회

    @GET("/medicine/case")
    Call<String> getMedicineByBarcode(@Header("authorization") String jwt, @Query("barcode") String barcode);   // 바코드 정보로 의약품 정보 조회

    @GET("/medicine/voice")
    Call<String> getMedicineVoice(@Query("name") String medicineName);                              // 음성으로 의약품명 조회

    @GET("/medicine/prescription")
    Call<String> getMedicineByPrescription(@Header("authorization") String jwt, @Query("names") String medicineList);   // 처방전으로 의약품명 조회


    @GET("/user/favorites")
    Call<String> getFavoritesList(@Header("authorization") String jwt);                             // 회원의 즐겨찾기 목록 조회
}
