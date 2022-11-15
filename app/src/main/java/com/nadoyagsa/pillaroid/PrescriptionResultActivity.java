package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrescriptionResultActivity extends AppCompatActivity {
    private ArrayList<PrescriptionInfo> prescriptionInfos;
    private ArrayList<String> medicineList;

    private PrescriptionPagerAdapter prescriptionPagerAdapter;
    private TextView tvResultNum, tvCurrentNum, tvResultNum2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_result);

        medicineList = getIntent().getStringArrayListExtra("medicineList");
        prescriptionInfos = new ArrayList<>();

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

        //로그인 후에 툴바가 바뀌어야 함(로그아웃 버튼이 보임)
        ActivityResultLauncher<Intent> startActivityResultLogin = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                tts.speak("로그인 되셨습니다. 이후부터 즐겨찾기 추가가 가능합니다.", QUEUE_FLUSH, null, null);
                getPrescriptionResult();
            }
            else {
                tts.speak("로그인에 문제가 발생해 즐겨찾기 기능 사용이 불가합니다.", QUEUE_FLUSH, null, null);
            }
        });

        ViewPager2 vpResult = findViewById(R.id.vp_prescription_result);
        prescriptionPagerAdapter = new PrescriptionPagerAdapter(startActivityResultLogin, prescriptionInfos);
        vpResult.setAdapter(prescriptionPagerAdapter);
        vpResult.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tts.speak(position+1+"번째 약품", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(500, QUEUE_ADD, null);
                tts.speak(prescriptionInfos.get(position).getName(), QUEUE_ADD, null, null);

                tvCurrentNum.setText(String.valueOf(position+1));
                super.onPageSelected(position);
            }
        });

        getPrescriptionResult();
    }

    private void getPrescriptionResult() {
        prescriptionInfos.clear();
        PillaroidAPIImplementation.getApiService().getMedicineByPrescription(SharedPrefManager.read("token", null), String.join(",", medicineList)).enqueue(new Callback<String>() {
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

                                JSONObject appearance = medicine.getJSONObject("appearance");
                                String feature = null, formulation = null, shape = null, color = null, dividingLine = null, identificationMark = null;

                                boolean isAppearanceNull = appearance.isNull("feature") && appearance.isNull("formulation") && appearance.isNull("shape")
                                        && appearance.isNull("color") && appearance.isNull("dividingLine") && appearance.isNull("identificationMark");

                                if (!appearance.isNull("feature"))
                                    feature = appearance.getString("feature");
                                if (!appearance.isNull("formulation"))
                                    formulation = appearance.getString("formulation");
                                if (!appearance.isNull("shape"))
                                    shape = appearance.getString("shape");
                                if (!appearance.isNull("color"))
                                    color = appearance.getString("color");
                                if (!appearance.isNull("dividingLine"))
                                    dividingLine = appearance.getString("dividingLine");
                                if (!appearance.isNull("identificationMark"))
                                    identificationMark = appearance.getString("identificationMark");

                                AppearanceInfo appearanceInfo = new AppearanceInfo(feature, formulation, shape, color, dividingLine, identificationMark);
                                appearanceInfo.setIsNull(isAppearanceNull);

                                Long favoritesIdx = medicine.isNull("favoritesIdx") ? null : medicine.getLong("favoritesIdx");

                                prescriptionInfos.add(new PrescriptionInfo(medicine.getInt("medicineIdx"), name,
                                        appearanceInfo, medicine.getString("efficacy"), medicine.getString("dosage"), favoritesIdx));
                            }
                            prescriptionPagerAdapter.notifyDataSetChanged();

                            tts.speak("조회된 처방 의약품은 ", QUEUE_FLUSH, null, null);
                            for (PrescriptionInfo prescriptionInfo: prescriptionInfos) {
                                tts.speak(prescriptionInfo.getName(), QUEUE_ADD, null, null);
                                tts.playSilentUtterance(200, QUEUE_ADD, null);
                            }
                            tts.speak("로, 총 "+prescriptionInfos.size()+"개 입니다.", TextToSpeech.QUEUE_ADD, null, null);
                            tts.playSilentUtterance(5000, QUEUE_ADD, null);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else {
                    tts.speak("처방전 결과 조회에 문제가 생겼습니다. Return to the previous screen.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, QUEUE_ADD, null);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("Can't connect to server. Return to the previous screen.", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(5000, QUEUE_ADD, null);
                finish();
            }
        });
    }
}
