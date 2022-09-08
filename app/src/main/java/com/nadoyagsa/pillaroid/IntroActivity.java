package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class IntroActivity extends AppCompatActivity {
    private final String INTRO_DESCRIPTION = "intro-description";

    private TextView tvDescription;
    private long delay = 0;
    private View currentClickedView = null;

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

        tts.speak(tvDescription.getText(), QUEUE_FLUSH, null, INTRO_DESCRIPTION);
        setClickListener();
    }

    public void setClickListener() {
        AppCompatButton btNext = findViewById(R.id.bt_intro_next);
        btNext.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 2000;
                tts.speak("버튼." + getString(R.string.page_medicine_favorites), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v){
                finish();   // MainActivity로 돌아감
            }
        });

        TextView tvDescription = findViewById(R.id.tv_intro_description);
        tvDescription.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });
    }
}
