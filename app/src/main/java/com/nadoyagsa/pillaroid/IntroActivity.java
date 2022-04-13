package com.nadoyagsa.pillaroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class IntroActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String guideValue = preferences.getString("guideVolume", "8");  //음량

        //설정한 음량값으로 음량 조절 (android.intent.action.MAIN에서 항상 설정 요망)
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.parseInt(guideValue), AudioManager.FLAG_SHOW_UI);

        AppCompatButton btNext = findViewById(R.id.bt_intro_next);
        btNext.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
