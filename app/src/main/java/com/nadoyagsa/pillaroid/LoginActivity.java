package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private long delay = 0;
    private View currentClickedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fromWhere = getIntent().getCharExtra("from", 'a');

        setClickListener();
    }

    //카카오 로그인 콜백 함수
    Function2<OAuthToken, Throwable, Unit> kakaoLoginCallback = (oAuthToken, throwable) -> {
        if (oAuthToken != null) {
            JsonObject kakaoToken = new JsonObject();
            kakaoToken.addProperty("access_token", oAuthToken.getAccessToken());
            kakaoToken.addProperty("alarm_token", SharedPrefManager.read("alarm_token", ""));
            PillaroidAPIImplementation.getApiService().postLogin(kakaoToken).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200 || response.code() == 201) {
                        try {
                            isLogin = true;

                            JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                            JSONObject data = responseJson.getJSONObject("data");
                            String token = data.getString("authToken");

                            // 토큰을 저장함
                            SharedPrefManager.write("token", token);

                            if (fromWhere == 'f') {
                                tts.speak("You are logged in. Go to the Favorites list screen.", QUEUE_FLUSH, null, null);
                                startActivity(new Intent(LoginActivity.this, MypageFavoritesActivity.class));
                            }
                            else if (fromWhere == 'a') {
                                tts.speak("You are logged in. You will be taken to the notification list screen.", QUEUE_FLUSH, null, null);
                                startActivity(new Intent(LoginActivity.this, MypageAlarmActivity.class));
                            }

                            finish();
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    else {
                        tts.speak("There is a problem with Kakao login. Please log in again.", QUEUE_FLUSH, null, null);
                        Toast.makeText(LoginActivity.this, "There is a problem with Kakao login. Please log in again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("Can't connect to server. Please check.", QUEUE_FLUSH, null, null);
                    Toast.makeText(LoginActivity.this, "Can't connect to server. Please check.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if (throwable != null) {
            tts.speak("There is a problem with Kakao login. Please log in again.", QUEUE_FLUSH, null, null);
            Toast.makeText(LoginActivity.this, "There is a problem with Kakao login. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        return null;
    };

    @Override
    public void finish() {
        if (isLogin) {
            Intent loginIntent = new Intent();
            loginIntent.putExtra("to", fromWhere);
            setResult(RESULT_OK, loginIntent);
        }
        else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
        }

        super.finish();
    }

    public void setClickListener() {
        LinearLayout llKakaoLogin = findViewById(R.id.ll_login_kakao);
        llKakaoLogin.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("Button. Log in to Kakao.", QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                // 카카오 로그인 연동
                if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this))
                    UserApiClient.getInstance().loginWithKakaoTalk(this, kakaoLoginCallback);
                else
                    UserApiClient.getInstance().loginWithKakaoAccount(this, kakaoLoginCallback);
            }
        });
    }
}
