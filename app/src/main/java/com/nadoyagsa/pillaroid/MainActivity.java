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

import com.google.firebase.messaging.FirebaseMessaging;
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

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.ENGLISH);
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
                    tts.speak("The pillaroid app has been launched.", QUEUE_FLUSH, null, null);
                }
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("fcm", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    if (!SharedPrefManager.read("alarm_token", "").equals("") || !SharedPrefManager.read("alarm_token", "").equals(token)) {
                        SharedPrefManager.remove("alarm_token");
                    }
                    SharedPrefManager.write("alarm_token", token);
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
                        tts.speak("Access by unauthorized members.", QUEUE_FLUSH, null, null);
                    }
                    else if (response.code() == 400) {
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                JSONObject errorBody = new JSONObject(errorStr);
                                long errorIdx = errorBody.getLong("errorIdx");

                                if (errorIdx == 40001)
                                    tts.speak("There is no alarm token.", QUEUE_FLUSH, null, null);
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                            tts.speak("There was a problem setting the token for the alarm.", QUEUE_FLUSH, null, null);
                    }
                    else {
                        tts.speak("There was a problem setting the token for the alarm.", QUEUE_FLUSH, null, null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("Can't connect to server.", QUEUE_FLUSH, null, null);
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
                tts.speak("Button." + getString(R.string.page_search_camera), QUEUE_FLUSH, null, null);
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
                tts.speak("Button." + getString(R.string.page_search_voice), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, SearchVoiceActivity.class));
            }
        });

        LinearLayout llPrescription = findViewById(R.id.ll_main_document);
        llPrescription.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("Button." + getString(R.string.page_search_document), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, SearchDocumentActivity.class));
            }
        });

        LinearLayout llMypage = findViewById(R.id.ll_main_mypage);
        llMypage.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("Button." + getString(R.string.page_mypage), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                if (isReadyTts)
                    startActivity(new Intent(this, MypageActivity.class));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        tts.speak("main screen", QUEUE_FLUSH, null, null);
    }
}
