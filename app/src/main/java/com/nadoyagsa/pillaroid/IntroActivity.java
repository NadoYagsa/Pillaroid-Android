package com.nadoyagsa.pillaroid;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class IntroActivity extends AppCompatActivity {
    private final String INTRO_DESCRIPTION = "intro-description";

    TextView tvDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        tvDescription = findViewById(R.id.tv_intro_description);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals(INTRO_DESCRIPTION)) {
                    finish();   //tts speak 끝나면 MainActivity로 돌아가기
                }
            }

            @Override
            public void onError(String utteranceId) { }
        });

        tts.speak(tvDescription.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, INTRO_DESCRIPTION);

        AppCompatButton btNext = findViewById(R.id.bt_intro_next);
        btNext.setOnClickListener(v -> {
            finish();   // MainActivity로 돌아감
        });
    }
}
