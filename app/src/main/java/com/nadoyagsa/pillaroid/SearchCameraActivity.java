package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;
import java.util.Objects;

public class SearchCameraActivity extends AppCompatActivity {
    public static final int RESULT_PERMISSION_DENIED = -1000;
    private static final int REQUEST_CAMERA_ACTIVITY = 1;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_camera);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        Toolbar toolbar = findViewById(R.id.tb_search_camera_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        /* 액션바 아이콘과 주제 설정 */
        ImageView ivIcon = customView.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_camera);
        TextView tvTopic = customView.findViewById(R.id.tv_ab_icontext_title);
        tvTopic.setText("의약품 촬영으로 검색");
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        LinearLayout llSearchCase = findViewById(R.id.ll_search_camera_case);
        llSearchCase.setOnClickListener(v -> startActivityForResult(new Intent(this, SearchCaseActivity.class), REQUEST_CAMERA_ACTIVITY));

        LinearLayout llSearchPill = findViewById(R.id.ll_search_camera_pill);
        llSearchPill.setOnClickListener(v -> startActivity(new Intent(this, SearchPillActivity.class)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAMERA_ACTIVITY && resultCode == RESULT_PERMISSION_DENIED) {
            tts.speak("카메라 권한이 승인되지 않아 기능을 사용할 수 없습니다.", QUEUE_FLUSH, null, null);
        }
    }
}
