package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;
import java.util.Objects;

public class SearchVoiceActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_voice);

        Toolbar toolbar = findViewById(R.id.tb_voicesearch_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

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

                //TODO: layout text들 string.xml에 넣으면 getString(R.string._)로 바꾸기
                tts.speak("검색할 의약품 이름을 볼륨 버튼을 누르고 말해주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("녹음 시작 시 '띵똥' 소리가 들립니다.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("말하기를 완료하셨다면 볼륨 버튼을 다시 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("재녹음도 동일한 방법으로 진행됩니다.", TextToSpeech.QUEUE_ADD, null, null);
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        AppCompatButton btResult = findViewById(R.id.bt_voicesearch_result);
        btResult.setOnClickListener(v -> startActivity(new Intent(this, VoiceResultsActivity.class)));
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_voice);
        ivIcon.setContentDescription("녹음기 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText("의약품 음성으로 검색");
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
