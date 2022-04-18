package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.util.Locale;

public class IntroActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        //TODO: android.intent.action.MAIN 인 Activity 에 있어야 함 (추후 MainActivity 로 옮길 예정)
        SharedPrefManager.init(getApplicationContext());  //SharedPreferences 싱글톤
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

                TextView tvDescription = findViewById(R.id.tv_intro_description);
                tts.speak(tvDescription.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);

                //TODO: tts speak 끝나면 MainActivity 로 넘어가기
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        AppCompatButton btNext = findViewById(R.id.bt_intro_next);
        btNext.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
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
