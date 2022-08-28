package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
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
    private boolean isLogin = false;
    private char fromWhere;     //f는 즐겨찾기 목록 확인, a는 알람 목록 확인

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fromWhere = getIntent().getCharExtra("from", 'a');

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
                            isLogin = true;
                            JSONObject loginInfo = new JSONObject(Objects.requireNonNull(response.body()));
                            String token = loginInfo.getString("authToken");

                            // 토큰을 저장함
                            SharedPrefManager.write("token", token);

                            if (fromWhere == 'f') {
                                tts.speak("로그인 되셨습니다. 즐겨찾기 목록 화면으로 넘어갑니다.", QUEUE_FLUSH, null, null);
                                startActivity(new Intent(LoginActivity.this, MypageFavoritesActivity.class));
                            }
                            else if (fromWhere == 'a') {
                                tts.speak("로그인 되셨습니다. 알림 목록 화면으로 넘어갑니다.", QUEUE_FLUSH, null, null);
                                startActivity(new Intent(LoginActivity.this, MypageAlarmActivity.class));
                            }

                            finish();
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    else {
                        tts.speak("카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", QUEUE_FLUSH, null, null);
                        Toast.makeText(LoginActivity.this, "카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("서버와 연결이 되지 않습니다. 확인해 주세요.", QUEUE_FLUSH, null, null);
                    Toast.makeText(LoginActivity.this, "서버와 연결이 되지 않습니다. 확인해 주세요:)", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if (throwable != null) {
            tts.speak("카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", QUEUE_FLUSH, null, null);
            Toast.makeText(LoginActivity.this, "카카오 로그인에 문제가 생겼습니다. 다시 로그인 부탁드립니다.", Toast.LENGTH_SHORT).show();
        }

        return null;
    };

    @Override
    public void finish() {
        if (isLogin) {
            Intent loginIntent = new Intent();
            setResult(RESULT_OK, loginIntent);
        }
        else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
        }

        super.finish();
    }
}
