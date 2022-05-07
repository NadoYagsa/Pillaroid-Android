package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.nadoyagsa.pillaroid.adapter.VoiceResultsListAdapter;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class VoiceResultsActivity extends AppCompatActivity {
    private String voiceQuery = null;

    private TextToSpeech tts;
    private int currentIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_results);

        voiceQuery = getIntent().getStringExtra("query");   //검색 의약품명

        //의약품 결과 샘플
        ArrayList<VoiceResultInfo> voiceResults = new ArrayList<>();
        voiceResults.add(new VoiceResultInfo(202005623L, "어린이타이레놀산160밀리그램"));
        voiceResults.add(new VoiceResultInfo(202106092L, "타이레놀정500밀리그람"));
        voiceResults.add(new VoiceResultInfo(202106954L, "타이레놀콜드-에스정"));
        voiceResults.add(new VoiceResultInfo(202200407L, "타이레놀8시간이알서방정"));
        voiceResults.add(new VoiceResultInfo(202200525L, "어린이타이레놀현탁액"));
        voiceResults.add(new VoiceResultInfo(202200658L, "우먼스타이레놀정"));
        voiceResults.add(new VoiceResultInfo(202201300L, "타이레놀산500밀리그램"));

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));
                
                //차례대로 리스트 읽어주기
                tts.speak("조회된 의약품을 음성으로 나열합니다. 원하시는 결과가 나오면 볼륨 버튼을 눌러주세요.", QUEUE_FLUSH, null, null);
                for (int i = 0; i < voiceResults.size(); i++) {
                    tts.speak(i+1 + "번. " + voiceResults.get(i).getMedicineName(), QUEUE_ADD, null, "results " + i);
                    tts.playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, "results " + i);   //1초 딜레이
                }
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        Toolbar toolbar = findViewById(R.id.tb_voiceresults_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

        final TextView tvResultsIdx = findViewById(R.id.tv_voiceresults_idx);
        tvResultsIdx.setText("0");

        final TextView tvResultsCount = findViewById(R.id.tv_voiceresults_count);
        tvResultsCount.setText(String.valueOf(voiceResults.size()));

        final ListView lvResults = findViewById(R.id.lv_voiceresults_results);
        VoiceResultsListAdapter adapter = new VoiceResultsListAdapter(this, 0, voiceResults);
        lvResults.setAdapter(adapter);

        //UI 변경 (main thread에서 진행)
        final Handler focusItemHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                tvResultsIdx.setText(String.valueOf(msg.what + 1));

                adapter.setSelectedItem(msg.what);   //배경색 및 텍스트색 변경
                int offset = lvResults.getHeight() / 2 - 30;    //(ListView 높이 / 2) - (Item 높이 / 2)
                lvResults.setSelectionFromTop(msg.what, offset); //해당 아이템을 중앙으로 이동
            }
        };

        setListener(focusItemHandler);
    }

    private void setListener(Handler focusItemHandler) {
        //tts 리스너: voiceResults 항목을 자동으로 읽을 때 ListView에 읽고 있는 항목 표시
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (utteranceId.contains("results")) {
                    int idx = Integer.parseInt(utteranceId.split(" ")[1]);
                    if (currentIdx != idx) {    //딜레이로 인한 콜백이면 View 변경 X
                        currentIdx = idx;

                        //UI 변경할 수 있도록 handler에 message 전달
                        Message msg = focusItemHandler.obtainMessage(currentIdx);
                        focusItemHandler.sendMessage(msg);
                    }
                }
            }

            @Override
            public void onDone(String utteranceId) { }

            @Override
            public void onError(String utteranceId) { }
        });

        //TODO: 볼륨버튼 리스너
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_voice);
        ivIcon.setContentDescription("녹음기 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText("검색 결과");
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
