package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.nadoyagsa.pillaroid.adapter.VoiceResultsListAdapter;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceResultsActivity extends AppCompatActivity {
    private final String API_ERROR = "api-error";

    private long delay = 0;
    private int selectedPos = -1;       // currentClickedView 대신
    private TextView tvResultsIdx, tvResultsCount;

    private int currentIdx = -1;
    private String voiceQuery = null;
    private ArrayList<VoiceResultInfo> voiceResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_results);

        voiceResults = new ArrayList<>();   //의약품 결과 샘플

        Toolbar toolbar = findViewById(R.id.tb_voiceresults_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

        voiceQuery = getIntent().getStringExtra("query");   //검색 의약품명

        TextView tvMedicineName = findViewById(R.id.tv_voiceresults_name);
        tvMedicineName.setText(voiceQuery);
        tvMedicineName.setOnClickListener(v -> {
            selectedPos = -1;
            tts.speak("텍스트." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });

        if (voiceQuery != null)
            getVoiceResults();
        else {
            tts.speak("말하신 의약품명이 전달되지 않았습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
            tts.playSilentUtterance(2000, TextToSpeech.QUEUE_ADD, null);   // 2초 딜레이
            finish();
        }

        setClickListener();
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_voice);
        ivIcon.setContentDescription("녹음기 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText("검색 결과");
        tvTitle.setOnClickListener(v -> {
            selectedPos = -1;
            tts.speak("텍스트." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
    }

    private void showVoiceResults() {
        tvResultsIdx = findViewById(R.id.tv_voiceresults_idx);
        tvResultsIdx.setText("0");

        tvResultsCount = findViewById(R.id.tv_voiceresults_count);
        tvResultsCount.setText(String.valueOf(voiceResults.size()));

        ListView lvResults = findViewById(R.id.lv_voiceresults_results);
        VoiceResultsListAdapter adapter = new VoiceResultsListAdapter(this, 0, voiceResults);
        lvResults.setAdapter(adapter);
        lvResults.setOnItemClickListener((adapterView, view, position, id) -> {
            tts.stop();
            currentIdx = position;
            adapter.setSelectedItem(position);
            tvResultsIdx.setText(String.valueOf(position+1));

            if (System.currentTimeMillis() > delay) {
                selectedPos = position;
                delay = System.currentTimeMillis() + 2000;
                tts.speak(voiceResults.get(position).getMedicineName(), QUEUE_FLUSH, null, null);
            } else if (selectedPos == position) {
                Intent medicineIntent = new Intent(VoiceResultsActivity.this, MedicineResultActivity.class);
                medicineIntent.putExtra("medicineIdx", voiceResults.get(position).getMedicineIdx());
                startActivity(medicineIntent);
            }
        });

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

        //차례대로 리스트 읽어주기
        tts.speak("조회된 의약품을 음성으로 나열합니다. 원하시는 결과가 나오면 볼륨 버튼을 눌러주세요.", QUEUE_FLUSH, null, null);
        for (int i = 0; i < voiceResults.size(); i++) {
            tts.speak(i+1 + "번. " + voiceResults.get(i).getMedicineName(), QUEUE_ADD, null, "results " + i);
            tts.playSilentUtterance(1000, QUEUE_ADD, "results " + i);   //1초 딜레이
        }
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
            public void onDone(String utteranceId) {
                if (utteranceId.equals(API_ERROR)) {   //api 통신 중 오류가 생기면 이전 페이지로 돌아감
                    finish();
                }
            }

            @Override
            public void onError(String utteranceId) { }
        });
    }

    private void getVoiceResults() {
        voiceResults.clear();

        //TODO: voiceQuery 에서 마이크로그램과 밀리그램, 밀리리터, 밀리그람, 그램, 그람을 바꿔야 함
        /*
        String query = voiceQuery.replace("마이크로그램", "μg")
                .replace("마이크로그람", "μg")
                .replace("마이크로g", "μg");
         */

        PillaroidAPIImplementation.getApiService().getMedicineVoice(voiceQuery).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    try {
                        JSONObject voiceInfo = new JSONObject(Objects.requireNonNull(response.body()));

                        JSONArray results = voiceInfo.getJSONArray("data");
                        if (results.length() == 0)
                            tts.speak(voiceQuery.concat("에 대한 의약품 검색 결과가 없습니다."), QUEUE_FLUSH, null, null);
                        else {
                            for (int i=0; i<results.length(); i++) {
                                JSONObject medicine = results.getJSONObject(i);

                                String name = medicine.getString("name");
                                name = name.replace('[', '(')
                                        .replace(']', ')')
                                        .replaceAll("\\([^)]*\\)", "");

                                voiceResults.add(new VoiceResultInfo(medicine.getInt("medicineIdx"), name));
                            }
                            showVoiceResults();
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else {
                    tts.speak("음성 결과 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_ERROR);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_ERROR);
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 해당 번째의 의약품 정보 조회
                tts.stop();

                Intent medicineIntent = new Intent(this, MedicineResultActivity.class);
                medicineIntent.putExtra("medicineIdx", voiceResults.get(currentIdx).getMedicineIdx());
                startActivity(medicineIntent);

                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                // TODO: 추후에 결정

                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setClickListener() {
        TextView tvRule = findViewById(R.id.tv_voiceresults_rule);
        tvRule.setOnClickListener(v -> {
            selectedPos = -1;
            tts.speak("텍스트." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        LinearLayout llTotal = findViewById(R.id.ll_voiceresults_total);
        llTotal.setOnClickListener(v -> {
            selectedPos = -1;
            tts.speak("총 "+tvResultsCount.getText().toString()+"개 중 "+tvResultsIdx.getText().toString() + "개입니다.", QUEUE_FLUSH, null, null);
        });
    }
}
