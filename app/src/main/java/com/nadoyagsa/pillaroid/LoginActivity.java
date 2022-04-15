package com.nadoyagsa.pillaroid;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LinearLayout llKakaoLogin = findViewById(R.id.ll_login_kakao);
        llKakaoLogin.setOnClickListener(view -> {
            // 카카오 로그인 연동
            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this))
                UserApiClient.getInstance().loginWithKakaoTalk(this, kakaoLoginCallback);
            else
                UserApiClient.getInstance().loginWithKakaoAccount(this, kakaoLoginCallback);
        });
    }

    //카카오 로그인 콜백 함수
    Function2<OAuthToken, Throwable, Unit> kakaoLoginCallback = (oAuthToken, throwable) -> {
        if (oAuthToken != null) {
            JsonObject kakaoToken = new JsonObject();
            kakaoToken.addProperty("access_token", oAuthToken.getAccessToken());
            PillaroidAPIImplementation.getApiService().postLogin(kakaoToken).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200 || response.code() == 201) {
                        try {
                            JSONObject loginInfo = new JSONObject(Objects.requireNonNull(response.body()));
                            Long userIdx = loginInfo.getJSONObject("user").getLong("userIdx");
                            Log.i("확인용", loginInfo.toString());

                            //TODO: 클릭한 버튼이 무엇인지 확인(즐겨찾기? or 알람?)
                            //startActivity(new Intent(LoginActivity.this, MypageAlarmActivity.class));
                            //startActivity(new Intent(LoginActivity.this, MypageFavoritesActivity.class));
                            //finish();
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    else {
                        //TODO: 음성으로 출력해야 함!
                        Toast.makeText(LoginActivity.this, "카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    Toast.makeText(LoginActivity.this, "서버와 연결이 되지 않습니다. 확인해 주세요:)", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if (throwable != null) {
            Toast.makeText(LoginActivity.this, "카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", Toast.LENGTH_SHORT).show();
        }

        return null;
    };
}
