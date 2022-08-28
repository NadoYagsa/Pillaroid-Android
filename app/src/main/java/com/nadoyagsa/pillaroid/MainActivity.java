package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

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

import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static TextToSpeech tts;
    private boolean isReadyTts = false;

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

        Toolbar toolbar = findViewById(R.id.tb_main_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_main, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        LinearLayout llCamera = findViewById(R.id.ll_main_camera);
        llCamera.setOnClickListener(v -> {
            if (isReadyTts)
                startActivity(new Intent(this, SearchCameraActivity.class));
        });

        LinearLayout llVoice = findViewById(R.id.ll_main_voice);
        llVoice.setOnClickListener(v -> {
            if (isReadyTts)
                startActivity(new Intent(this, SearchVoiceActivity.class));
        });

        LinearLayout llPrescription = findViewById(R.id.ll_main_prescription);
        llPrescription.setOnClickListener(v -> {
            if (isReadyTts)
                startActivity(new Intent(this, SearchPrescriptionActivity.class));
        });

        LinearLayout llMypage = findViewById(R.id.ll_main_mypage);
        llMypage.setOnClickListener(v -> {
            if (isReadyTts)
                startActivity(new Intent(this, MypageActivity.class));
        });
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
}
