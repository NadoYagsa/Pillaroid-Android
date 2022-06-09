package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.nadoyagsa.pillaroid.adapter.PrescriptionPagerAdapter;
import com.nadoyagsa.pillaroid.data.AppearanceInfo;
import com.nadoyagsa.pillaroid.data.PrescriptionInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrescriptionResultActivity extends AppCompatActivity {
    private ArrayList<PrescriptionInfo> prescriptionInfos;

    private PrescriptionPagerAdapter prescriptionPagerAdapter;
    private TextToSpeech tts;
    private TextView tvResultNum, tvCurrentNum, tvResultNum2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_result);

        ArrayList<String> medicineList = getIntent().getStringArrayListExtra("medicineList");
        prescriptionInfos = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                getPrescriptionResult(String.join(",", medicineList));
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        Toolbar toolbar = findViewById(R.id.tb_prescription_result_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_prescription, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        tvResultNum = toolbar.findViewById(R.id.tv_ab_prescription_result_num);

        tvCurrentNum = findViewById(R.id.tv_prescription_result_pos);
        tvResultNum2 = findViewById(R.id.tv_prescription_result_num);

        ViewPager2 vpResult = findViewById(R.id.vp_prescription_result);
        prescriptionPagerAdapter = new PrescriptionPagerAdapter(prescriptionInfos);
        vpResult.setAdapter(prescriptionPagerAdapter);
        vpResult.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tts.speak(position+1+"번째 약품", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null);
                tts.speak(prescriptionInfos.get(position).getMedicineName(), QUEUE_ADD, null, null);

                tvCurrentNum.setText(String.valueOf(position+1));
                super.onPageSelected(position);
            }
        });
    }

    private void getPrescriptionResult(String medicineList) {
        prescriptionInfos.clear();
        PillaroidAPIImplementation.getApiService().getMedicineByPrescription(medicineList).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    try {
                        JSONObject voiceInfo = new JSONObject(Objects.requireNonNull(response.body()));
                        JSONArray results = voiceInfo.getJSONArray("data");
                        
                        tvResultNum.setText(String.valueOf(results.length()));      // 검색 결과 수 출력
                        tvResultNum2.setText(String.valueOf(results.length()));
                        
                        if (results.length() == 0)
                            tts.speak("처방전에 명시된 의약품의 검색 결과가 없습니다.", QUEUE_FLUSH, null, null);
                        else {
                            for (int i=0; i<results.length(); i++) {
                                JSONObject medicine = results.getJSONObject(i);

                                String name = medicine.getString("name");
                                name = name.replace('[', '(')
                                        .replace(']', ')')
                                        .replaceAll("\\([^)]*\\)", "");

                                JSONObject jsonObject = new JSONObject(medicine.getString("appearanceInfo"));
                                String appearance = null, formulation = null, shape = null, color = null, dividingLine = null, identificationMark = null;

                                if (jsonObject.has("appearance"))
                                    appearance = jsonObject.getString("appearance");
                                if (jsonObject.has("formulation"))
                                    formulation = jsonObject.getString("formulation");
                                if (jsonObject.has("shape"))
                                    shape = jsonObject.getString("shape");
                                if (jsonObject.has("color"))
                                    color = jsonObject.getString("color");
                                if (jsonObject.has("dividingLine"))
                                    dividingLine = jsonObject.getString("dividingLine");
                                if (jsonObject.has("identificationMark"))
                                    identificationMark = jsonObject.getString("identificationMark");

                                AppearanceInfo appearanceInfo = new AppearanceInfo(appearance, formulation, shape, color, dividingLine, identificationMark);

                                prescriptionInfos.add(new PrescriptionInfo(medicine.getLong("idx"), name,
                                        appearanceInfo, medicine.getString("efficacy"), medicine.getString("usage")));
                            }
                            prescriptionPagerAdapter.notifyDataSetChanged();

                            tts.speak("조회된 처방 의약품은 ", TextToSpeech.QUEUE_FLUSH, null, null);
                            for (PrescriptionInfo prescriptionInfo: prescriptionInfos) {
                                tts.speak(prescriptionInfo.getMedicineName(), TextToSpeech.QUEUE_ADD, null, null);
                                tts.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, null);
                            }
                            tts.speak("로, 총 "+prescriptionInfos.size()+"개 입니다.", TextToSpeech.QUEUE_ADD, null, null);
                            tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else {
                    tts.speak("처방전 결과 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);
                finish();
            }
        });
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
