package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String guideValue = preferences.getString("guideVolume", "8");  //음량
        String voiceSpeed = preferences.getString("voiceSpeed", "1");   //음성 속도

        //설정한 음량값으로 음량 조절 (android.intent.action.MAIN에서 항상 설정 요망)
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.parseInt(guideValue), AudioManager.FLAG_SHOW_UI);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(Float.parseFloat(voiceSpeed));

                TextView tvDescription = findViewById(R.id.tv_intro_description);
                tts.speak(tvDescription.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
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
