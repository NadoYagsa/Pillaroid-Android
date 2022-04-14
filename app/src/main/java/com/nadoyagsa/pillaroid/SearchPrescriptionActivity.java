package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SearchPrescriptionActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_prescription);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String voiceSpeed = preferences.getString("voiceSpeed", "1");   //음성 속도

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(Float.parseFloat(voiceSpeed));

                tts.speak("후면 카메라가 켜졌습니다. 처방전을 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        /* TODO: 처방전 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, PrescriptionResultActivity.class));
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
