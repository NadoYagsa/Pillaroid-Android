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

public class SearchDocumentActivity extends AppCompatActivity {
    private long delay = 0;
    private View currentClickedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_document);

        Toolbar toolbar = findViewById(R.id.tb_search_document_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        /* 액션바 아이콘과 주제 설정 */
        ImageView ivIcon = customView.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_document);
        TextView tvTopic = customView.findViewById(R.id.tv_ab_icontext_title);
        tvTopic.setText(getString(R.string.page_search_document));
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        setClickListener();
    }

    public void setClickListener() {
        LinearLayout llSearchPrescription = findViewById(R.id.ll_search_document_prescription);
        llSearchPrescription.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_search_prescription), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                startActivity(new Intent(this, SearchPrescriptionActivity.class));
            }
        });

        LinearLayout llSearchPharmacy = findViewById(R.id.ll_search_document_pharmacy);
        llSearchPharmacy.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼." + getString(R.string.page_search_pharmacy_envelope), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                startActivity(new Intent(this, SearchPharmacyEnvelopeActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tts.speak("문서 선택 화면", QUEUE_FLUSH, null, null);
    }
}
