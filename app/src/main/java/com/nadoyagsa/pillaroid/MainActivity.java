package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    public static TextToSpeech tts;
    private boolean isReadyTts = false;

    private long delay = 0;
    private View currentClickedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPrefManager.init(getApplicationContext());  //SharedPreferences 싱글톤
        // 음량 조절
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);    //설정한 음량값으로 음량 조절
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefManager.read("guideVolume", 8), AudioManager.FLAG_SHOW_UI);

        SharedPrefManager.write("isFirstConnection", true);
        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));
                isReadyTts = true;

                // 첫 접속이라면 IntroActivity를 보여줌
                boolean isFirstConnection = SharedPrefManager.read("isFirstConnection", true);
                if (isFirstConnection) {
                    SharedPrefManager.write("isFirstConnection", false);
                    startActivity(new Intent(this, IntroActivity.class));
                } else {
                    tts.speak("pillaroid 앱이 실행되었습니다.", QUEUE_FLUSH, null, null);
                }
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        //알림 토큰 전달
        if (!SharedPrefManager.read("token", "").equals("") && !SharedPrefManager.read("alarm_token", "").equals("")) {
            JsonObject requestData = new JsonObject();
            requestData.addProperty("token", SharedPrefManager.read("alarm_token", ""));
            PillaroidAPIImplementation.getApiService().patchAlarmToken(SharedPrefManager.read("token", null), requestData).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200);
                    else if (response.code() == 401) {
                        tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                    }
                    else if (response.code() == 400) {
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                JSONObject errorBody = new JSONObject(errorStr);
                                long errorIdx = errorBody.getLong("errorIdx");

                                if (errorIdx == 40001)
                                    tts.speak("알람 토큰이 없습니다.", QUEUE_FLUSH, null, null);
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                            tts.speak("알람을 위한 토큰 설정에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                    }
                    else {
                        tts.speak("알람을 위한 토큰 설정에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                }
            });
        }

        Toolbar toolbar = findViewById(R.id.tb_main_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_main, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        setClickListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //tts 자원 해제
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setClickListener() {
        LinearLayout llCamera = findViewById(R.id.ll_main_camera);
        llCamera.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_search_camera), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, SearchCameraActivity.class));
            }
        });

        LinearLayout llVoice = findViewById(R.id.ll_main_voice);
        llVoice.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_search_voice), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, SearchVoiceActivity.class));
            }
        });

        LinearLayout llPrescription = findViewById(R.id.ll_main_prescription);
        llPrescription.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_search_prescription), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, SearchPrescriptionActivity.class));
            }
        });

        LinearLayout llMypage = findViewById(R.id.ll_main_mypage);
        llMypage.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_mypage), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, MypageActivity.class));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        tts.speak("메인 화면", QUEUE_FLUSH, null, null);
    }
}
