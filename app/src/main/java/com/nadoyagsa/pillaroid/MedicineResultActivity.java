package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;

import com.nadoyagsa.pillaroid.data.MedicineInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicineResultActivity extends AppCompatActivity {
    private Long medicineIdx = 0L;
    private MedicineInfo medicine;

    private View selectedCategoryView;

    private AlertDialog dialog;
    private AppCompatImageButton ivAlarm;
    private TextToSpeech tts;
    private TextView tvTitle, tvCategory, tvContent;
    private View dialogView;

    //TODO: 1. 카테고리 수평방향으로 스크롤 가능하게 해야함
    //TODO: 2. 즐겨찾기
    //TODO: 3. 알람

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

        //TODO: if문으로 idx, name 구분하시오!
        if (getIntent().hasExtra("medicineIdx"))
            medicineIdx = getIntent().getLongExtra("medicineIdx", 0L);   //검색 의약품 idx

        Toolbar toolbar = findViewById(R.id.tb_medicineresult_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_medicine_result, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                getMedicineResult();
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        dialogView = getLayoutInflater().inflate(R.layout.dialog_add_alarm, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        selectedCategoryView = findViewById(R.id.tv_medicineresult_efficacy);
        tvCategory = findViewById(R.id.tv_medicineresult_category);
        tvContent = findViewById(R.id.tv_medicineresult_content);

        clickListener();
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_medicineresult_icon);
        ivIcon.setImageResource(R.drawable.icon_info);
        ivIcon.setContentDescription("정보안내 아이콘");

        tvTitle = toolbar.findViewById(R.id.tv_ab_medicineresult_title);
        tvTitle.setText("");
        tvTitle.setSelected(true);  //ellipsize="marquee" 실행되도록 selected 설정

        AppCompatImageButton ibtStar = toolbar.findViewById(R.id.ibt_ab_medicineresult_star);
        ibtStar.setOnClickListener(v -> {
            //TODO: 즐겨찾기 관리 (ibtStar.tag=on/off)
        });

        ivAlarm = toolbar.findViewById(R.id.ibt_ab_medicineresult_alarm);   //TODO: 시각장애인 모드일 때 description 읽어주는지 확인하기
        ivAlarm.setOnClickListener(v -> showAlarmDialog());
    }

    public final void showAlarmDialog() {
        dialog.show();

        final EditText etLabel = dialogView.findViewById(R.id.et_dialog_addalarm_label);
        etLabel.setHint(medicine.getMedicineName());

        TextView tvCancel = dialogView.findViewById(R.id.tv_dialog_addalarm_cancel);
        if (ivAlarm.getTag().equals("on")) {
            tvCancel.setText("삭제");
        }

        TextView tvOk = dialogView.findViewById(R.id.tv_dialog_addalarm_ok);
        tvOk.setOnClickListener(v -> {
            String label = etLabel.getText().toString().equals("") ? etLabel.getHint().toString() : etLabel.getText().toString();
            int days = Integer.parseInt(((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).getText().toString());

            tts.speak(label + " 이름으로 알림이 생성되었습니다. 복용 기간은 " + days + "일 입니다.", QUEUE_FLUSH, null, null);

            //TODO: days 값이 유효하면 서버에 알림 내용 저장

            dialog.hide();
            ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_on));
            ivAlarm.setTag("on");
        });

        tvCancel.setOnClickListener(v -> {
            if (ivAlarm.getTag().equals("on")) {
                tts.speak("알림이 삭제되었습니다.", QUEUE_FLUSH, null, null);

                //TODO: 서버에 알림 삭제 요청 (사용자 및 의약품 품목일련번호 통해 삭제)

                ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_off));
                ivAlarm.setTag("off");
                ((TextView)v).setText("취소");
            }
            etLabel.setText("");
            ((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).setText("");
            dialog.dismiss();
        });
    }

    private void clickListener() {
        TextView tvEfficacy = findViewById(R.id.tv_medicineresult_efficacy);        // 효능 및 효과
        tvEfficacy.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvEfficacy.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_efficacy));
                tvContent.setText(medicine.getEfficacy());
            }
        });
        TextView tvUsage = findViewById(R.id.tv_medicineresult_usage);              // 용법 및 용량
        tvUsage.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvUsage.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_usage));
                tvContent.setText(medicine.getUsage());
            }
        });
        TextView tvPrecautions = findViewById(R.id.tv_medicineresult_precautions);   // 주의사항
        tvPrecautions.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvPrecautions.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_precautions));
                tvContent.setText(medicine.getPrecautions());
            }
        });
        TextView tvAppearance = findViewById(R.id.tv_medicineresult_appearance);    // 외형
        tvAppearance.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvAppearance.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_appearance));
                tvContent.setText(medicine.getAppearance());
            }
        });
        TextView tvIngredient = findViewById(R.id.tv_medicineresult_ingredient);    // 성분
        tvIngredient.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvIngredient.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_ingredient));
                tvContent.setText(medicine.getIngredient());
            }
        });
        TextView tvSave = findViewById(R.id.tv_medicineresult_save);                // 저장 방법
        tvSave.setOnClickListener(view -> {
            if (selectedCategoryView != view) {
                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));

                selectedCategoryView = view;
                tvSave.setTextColor(getColor(R.color.main_color));

                tvCategory.setText(getString(R.string.category_save));
                tvContent.setText(medicine.getSave());
            }
        });
    }

    private void showMedicineResult() {
        tvTitle.setText(medicine.getMedicineName());
        tvCategory.setText(getString(R.string.category_efficacy));
        tvContent.setText(medicine.getEfficacy());
    }

    private void getMedicineResult() {
        if (!medicineIdx.equals(0L)) {      // 의약품 번호로 정보 조회
            PillaroidAPIImplementation.getApiService().getMedicineByIdx(medicineIdx).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200) {
                        try {
                            JSONObject medicineInfo = new JSONObject(Objects.requireNonNull(response.body()));
                            JSONObject data = medicineInfo.getJSONObject("data");

                            String name = data.getString("name");
                            name = name.replace('[', '(')
                                    .replace(']', ')')
                                    .replaceAll("\\([^)]*\\)", "");

                            medicine = new MedicineInfo(data.getLong("idx"), data.getLong("code"),
                                    name, data.getString("efficacy"), data.getString("usage"),
                                    data.getString("precautions"), data.getString("appearanceInfo"),
                                    data.getString("ingredient"), data.getString("save"));

                            showMedicineResult();
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    else if (response.code() == 404) {
                        tts.speak("해당 의약품은 존재하지 않습니다.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);   // 2초 딜레이
                        finish();
                    }
                    else {
                        tts.speak("음성 결과 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);   // 2초 딜레이
                        finish();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(2000, TextToSpeech.QUEUE_ADD, null);   // 2초 딜레이
                    finish();
                }
            });
        }
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
