package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class SearchCameraActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_camera);

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
        llSearchCase.setOnClickListener(v -> startActivity(new Intent(this, SearchCaseActivity.class)));

        LinearLayout llSearchPill = findViewById(R.id.ll_search_camera_pill);
        llSearchPill.setOnClickListener(v -> startActivity(new Intent(this, SearchPillActivity.class)));

    }

    @Override
    protected void onResume() {
        super.onResume();
        tts.speak("의약품 촬영 방식 선택 화면", QUEUE_FLUSH, null, null);
    }
}
