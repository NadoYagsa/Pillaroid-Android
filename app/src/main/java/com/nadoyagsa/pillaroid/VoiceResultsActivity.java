package com.nadoyagsa.pillaroid;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.VoiceResultsRecyclerAdapter;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import java.util.ArrayList;

public class VoiceResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_results);

        //의약품 결과 샘플
        ArrayList<VoiceResultInfo> voiceResults = new ArrayList<VoiceResultInfo>();
        voiceResults.add(new VoiceResultInfo(202005623L, "어린이타이레놀산160밀리그램"));
        voiceResults.add(new VoiceResultInfo(202106092L, "타이레놀정500밀리그람"));
        voiceResults.add(new VoiceResultInfo(202106954L, "타이레놀콜드-에스정"));
        voiceResults.add(new VoiceResultInfo(202200407L, "타이레놀8시간이알서방정"));
        voiceResults.add(new VoiceResultInfo(202200525L, "어린이타이레놀현탁액"));
        voiceResults.add(new VoiceResultInfo(202200658L, "우먼스타이레놀정"));
        voiceResults.add(new VoiceResultInfo(202201300L, "타이레놀산500밀리그램"));

        Toolbar toolbar = findViewById(R.id.tb_voiceresults_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

        final TextView tvResultsIdx = findViewById(R.id.tv_voiceresults_idx);
        final TextView tvResultsCount = findViewById(R.id.tv_voiceresults_count);

        tvResultsIdx.setText("0");
        tvResultsCount.setText(String.valueOf(voiceResults.size()));

        RecyclerView rvResults = findViewById(R.id.rv_voiceresults_results);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvResults.setLayoutManager(manager);
        VoiceResultsRecyclerAdapter voiceResultsRecyclerAdapter = new VoiceResultsRecyclerAdapter(voiceResults);
        rvResults.setAdapter(voiceResultsRecyclerAdapter);
        rvResults.setItemAnimator(null);


    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_voice);
        ivIcon.setContentDescription("녹음기 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText("검색 결과");
    }
}
