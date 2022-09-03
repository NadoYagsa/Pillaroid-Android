package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.JsonObject;
import com.nadoyagsa.pillaroid.adapter.MedicinePagerAdapter;
import com.nadoyagsa.pillaroid.data.AppearanceInfo;
import com.nadoyagsa.pillaroid.data.MedicineInfo;
import com.nadoyagsa.pillaroid.data.AlarmInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicineResultActivity extends AppCompatActivity {
    private final long RESPONSE_BARCODE_FORMAT_ERROR = 40003L;
    private final long RESPONSE_BARCODE_NOT_FOUND = 40401L;
    private final long RESPONSE_MEDICINE_NOT_FOUND = 40402L;
    private final String API_FAILED = "api-failed";

    private int medicineIdx = 0;
    private String barcode = "";
    private MedicineInfo medicine;
    private HashMap<Integer,View> categories;

    private ActivityResultLauncher<Intent> startActivityResultLogin;
    private AlertDialog dialog;
    private AppCompatImageButton ivAlarm, ibtStar;
    private MedicinePagerAdapter medicinePagerAdapter;
    private TextView tvTitle;
    private View dialogView, selectedCategoryView;
    private ViewPager2 vpResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

        if (getIntent().hasExtra("medicineIdx"))
            medicineIdx = getIntent().getIntExtra("medicineIdx", 0);   //검색 의약품 idx
        else if (getIntent().hasExtra("barcode"))
            barcode = getIntent().getStringExtra("barcode");    //검색할 바코드

        getMedicineResult();

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

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals(API_FAILED)) {   //api 통신 중 오류가 생기면 이전 페이지로 돌아감
                    finish();
                }
            }

            @Override
            public void onError(String utteranceId) { }
        });

        dialogView = getLayoutInflater().inflate(R.layout.dialog_add_alarm, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        selectedCategoryView = findViewById(R.id.tv_medicineresult_efficacy);

        clickListener();

        medicine = new MedicineInfo(-1, "", "", "", "", null, "", "", null, null);
        vpResult = findViewById(R.id.vp_medicineresult_result);
        medicinePagerAdapter = new MedicinePagerAdapter(medicine);
        vpResult.setAdapter(medicinePagerAdapter);
        vpResult.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tts.speak(medicinePagerAdapter.getCategories().get(position), QUEUE_FLUSH, null, null);

                ((TextView) selectedCategoryView).setTextColor(getColor(R.color.black));
                ((TextView)selectedCategoryView).setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));

                selectedCategoryView = categories.get(position);
                assert selectedCategoryView != null;
                ((TextView)selectedCategoryView).setTextColor(getColor(R.color.main_color));
                ((TextView)selectedCategoryView).setTypeface(Typeface.DEFAULT_BOLD);

                vpResult.setCurrentItem(position);
                super.onPageSelected(position);
            }
        });

        // 즐겨찾기 or 알람 기능 사용 시 로그인이 안되었을 때
        startActivityResultLogin = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            String to = Objects.requireNonNull(result.getData()).getStringExtra("to");
            String type = to.equals("f") ? "즐겨찾기" : "알림";

            if (result.getResultCode() == RESULT_OK) {
                PillaroidAPIImplementation.getApiService().getFavoritesAndAlarm(SharedPrefManager.read("token", ""), medicine.getMedicineIdx()).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.code() == 200) {
                            try {
                                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                JSONObject data = responseJson.getJSONObject("data");

                                medicine.setFavoritesIdx(data.getLong("favoritesIdx"));

                                AlarmInfo alarmInfo = data.isNull("alarmResponse") ? null : new AlarmInfo(data.getJSONObject("alarmResponse"));
                                medicine.setAlarmInfo(alarmInfo);

                                tts.speak("로그인 되셨습니다. 이후부터 " + type + " 설정이 가능합니다.", TextToSpeech.QUEUE_FLUSH, null, null);

                                setIcon();     // 즐겨찾기, 알림 아이콘 설정
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (response.code() == 401) {
                            tts.speak("허가받지 않은 회원의 접근입니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                        } else {
                            tts.speak(type + " 여부 조회에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(3000, QUEUE_ADD, null);
                    }
                });
            }
            else {
                tts.speak("로그인에 문제가 발생해 " + type + " 기능 사용이 불가합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    private void initActionBar(Toolbar toolbar) {
        tvTitle = toolbar.findViewById(R.id.tv_ab_medicineresult_title);
        tvTitle.setText("");
        tvTitle.setSelected(true);  //ellipsize="marquee" 실행되도록 selected 설정

        // 즐겨찾기
        ibtStar = toolbar.findViewById(R.id.ibt_ab_medicineresult_favorites);
        ibtStar.setOnClickListener(view -> {
            if (SharedPrefManager.read("token", null) == null) {
                tts.speak("즐겨찾기 기능은 로그인이 필요합니다. 로그인을 하시려면 화면 하단의 카카오 로그인 버튼을 눌러주세요.", QUEUE_FLUSH, null, null);

                // 로그인 후에 툴바가 바뀌어야 함(로그아웃 버튼이 보임)
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("from", 'r');
                startActivityResultLogin.launch(loginIntent);
            }
            else {  // 이미 로그인된 사용자
                if (medicine.isFavoritesNull()) {   // 즐겨찾기 추가
                    JsonObject request = new JsonObject();
                    request.addProperty("medicineIdx", medicine.getMedicineIdx());
                    PillaroidAPIImplementation.getApiService().postFavorites(SharedPrefManager.read("token", null), request).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            if (response.code() == 201) {
                                try {
                                    JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                    JSONObject data = responseJson.getJSONObject("data");

                                    medicine.setFavoritesIdx(data.getLong("favoritesIdx"));
                                    tts.speak("즐겨찾기 추가", TextToSpeech.QUEUE_FLUSH, null, null);

                                    ((AppCompatImageButton) view).setImageResource(R.drawable.icon_star_on);
                                } catch (JSONException e) { e.printStackTrace(); }
                            }
                            else if (response.code() == 401) {
                                tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                            }
                            else {
                                tts.speak("즐겨찾기 추가에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                            tts.playSilentUtterance(3000, QUEUE_ADD, null);
                        }
                    });
                }
                else {                                              // 즐겨찾기 해제
                    PillaroidAPIImplementation.getApiService().deleteFavorites(SharedPrefManager.read("token", null), medicine.getFavoritesIdx()).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            if (response.code() == 200) {
                                medicine.setFavoritesIdx(null);
                                tts.speak("즐겨찾기 삭제", TextToSpeech.QUEUE_FLUSH, null, null);

                                ((AppCompatImageButton) view).setImageResource(R.drawable.icon_star_off);
                            }
                            else if (response.code() == 401) {
                                tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                            }
                            else if (response.code() == 400) {
                                if (response.errorBody() != null) {
                                    try {
                                        String errorStr = response.errorBody().string();
                                        JSONObject errorBody = new JSONObject(errorStr);
                                        long errorIdx = errorBody.getLong("errorIdx");

                                        if (errorIdx == 40001)  // 삭제 오류
                                            tts.speak("즐겨찾기에 추가되지 않은 의약품이기에 삭제가 불가합니다.", QUEUE_FLUSH, null, null);
                                    } catch (JSONException | IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else
                                    tts.speak("즐겨찾기 삭제에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                            }
                            else {
                                tts.speak("즐겨찾기 삭제에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                            tts.playSilentUtterance(3000, QUEUE_ADD, null);
                        }
                    });
                }
            }
        });

        // 알람
        ivAlarm = toolbar.findViewById(R.id.ibt_ab_medicineresult_alarm);   //TODO: 시각장애인 모드일 때 description 읽어주는지 확인하기
        ivAlarm.setOnClickListener(view -> {
            if (SharedPrefManager.read("token", null) == null) {
                tts.speak("알림 기능은 로그인이 필요합니다. 로그인을 하시려면 화면 하단의 카카오 로그인 버튼을 눌러주세요.", QUEUE_FLUSH, null, null);

                // 로그인 후에 툴바가 바뀌어야 함(로그아웃 버튼이 보임)
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("from", 'a');
                startActivityResultLogin.launch(loginIntent);
            } else {    // 이미 로그인된 사용자
                showAlarmDialog(medicine.getAlarmInfo());
            }
        });
    }

    public final void showAlarmDialog(AlarmInfo alarmInfo) {
        dialog.show();

        final EditText etLabel = dialogView.findViewById(R.id.et_dialog_addalarm_label);
        final EditText etDays = dialogView.findViewById(R.id.et_dialog_addalarm_days);

        if (alarmInfo == null) {
            tts.speak("알림 추가. 알림 이름과 복용 기간을 설정해주세요.", QUEUE_FLUSH, null, null);
            etLabel.setHint(medicine.getName());
        } else {
            tts.speak(String.format("현재 등록된 알림은 %s %d일 입니다.", alarmInfo.getName(), alarmInfo.getPeriod()), QUEUE_FLUSH, null, null);
            etLabel.setText(alarmInfo.getName());
            etDays.setText(String.valueOf(alarmInfo.getPeriod()));
        }

        TextView tvCancel = dialogView.findViewById(R.id.tv_dialog_addalarm_cancel);
        TextView tvOk = dialogView.findViewById(R.id.tv_dialog_addalarm_ok);
        if (ivAlarm.getTag().equals("on")) {
            tvOk.setText("삭제");
        }
        tvOk.setOnClickListener(v -> {
            if (ivAlarm.getTag().equals("off")) {        // 알림 추가
                if (etDays.getText().toString().equals("")) {
                    tts.speak("복용 기간을 설정해주세요.", QUEUE_FLUSH, null, null);
                    return;
                }

                int days = Integer.parseInt(etDays.getText().toString());
                if (days <= 0) {
                    tts.speak("복용 기간은 양수여야 합니다.", QUEUE_FLUSH, null, null);
                    return;
                }
                if (days > 92) {
                    tts.speak("최대 복용 알림 기간은 세 달입니다.", QUEUE_FLUSH, null, null);
                    return;
                }

                String label = etLabel.getText().toString().equals("") ? etLabel.getHint().toString() : etLabel.getText().toString();
                if (label.length() > 30) {
                    tts.speak("레이블이 너무 길어 30자까지만 저장합니다.", QUEUE_FLUSH, null, null);
                    label = label.substring(0, 30);
                }

                JsonObject request = new JsonObject();
                request.addProperty("medicineIdx", medicine.getMedicineIdx());
                request.addProperty("name", label);
                request.addProperty("period", days);

                PillaroidAPIImplementation.getApiService().postAlarm(SharedPrefManager.read("token", null),request).enqueue(new Callback<String>(){
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.code() == 201) {
                            try {
                                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                JSONObject data = responseJson.getJSONObject("data");

                                AlarmInfo alarmInfo = new AlarmInfo(data);
                                medicine.setAlarmInfo(alarmInfo);
                                tts.speak(alarmInfo.getName() + " 이름으로 알림이 생성되었습니다. 복용 기간은 " + alarmInfo.getPeriod() + "일 입니다.", QUEUE_FLUSH, null, null);

                                ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(MedicineResultActivity.this, R.drawable.icon_bell_on));
                                ivAlarm.setTag("on");
                                ((TextView) v).setText("삭제");
                                dialog.hide();
                            } catch (JSONException e) { e.printStackTrace(); }
                        } else if (response.code() == 400) {
                            if (response.errorBody() != null) {
                                try {
                                    String errorStr = response.errorBody().string();
                                    JSONObject errorBody = new JSONObject(errorStr);
                                    long errorIdx = errorBody.getLong("errorIdx");

                                    if (errorIdx == 40004) {        // 복용 시간대 없음
                                        tts.speak("복용 시간대가 설정되지 않았습니다. 마이페이지에서 설정해주세요.", QUEUE_FLUSH, null, null);
                                    } else if (errorIdx == 40005) { // 복용시기 파싱 정보 없음
                                        tts.speak("의약품에서 알림을 위한 정보를 얻을 수 없습니다.", QUEUE_FLUSH, null, null);
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                tts.speak("알림 추가에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                                dialog.dismiss();
                            }
                        } else if (response.code() == 401) {
                            tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                            dialog.dismiss();
                        } else if (response.code() == 409) {
                            tts.speak("이미 등록된 알림입니다.", QUEUE_FLUSH, null, null);
                            dialog.dismiss();
                        } else {
                            tts.speak("알림 추가에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                    }
                });
            } else if (ivAlarm.getTag().equals("on")) {        // 알림 삭제
                //서버에 알림 삭제 요청 (사용자 및 의약품 품목일련번호 통해 삭제)
                PillaroidAPIImplementation.getApiService().deleteAlarm(SharedPrefManager.read("token", null), alarmInfo.getAlarmIdx()).enqueue(new Callback<String>(){
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.code() == 200) {
                            tts.speak("알림이 삭제되었습니다.", QUEUE_FLUSH, null, null);
                            medicine.setAlarmInfo(null);
                        } else if (response.code() == 400) {
                            if (response.errorBody() != null) {
                                try {
                                    String errorStr = response.errorBody().string();
                                    JSONObject errorBody = new JSONObject(errorStr);
                                    long errorIdx = errorBody.getLong("errorIdx");

                                    if (errorIdx == 40001)  // 존재하지 않는 데이터
                                        tts.speak("알림에 추가되지 않은 의약품이기에 삭제가 불가합니다.", QUEUE_FLUSH, null, null);
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                                tts.speak("알림 삭제에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                        } else if (response.code() == 401) {
                            tts.speak("허가받지 않은 회원의 접근입니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                            finish();
                        } else {
                            Log.e("SOEUN-DEBUG", String.valueOf(response.code()));
                            tts.speak("알림 기능에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                    }
                });

                ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_off));
                ivAlarm.setTag("off");
                ((TextView) v).setText("등록");

                etLabel.setText("");
                etDays.setText("");
                dialog.dismiss();
            }
        });

        tvCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    private void clickListener() {
        TextView tvEfficacy = findViewById(R.id.tv_medicineresult_efficacy);        // 효능 및 효과
        tvEfficacy.setOnClickListener(view -> vpResult.setCurrentItem(0));

        TextView tvDosage = findViewById(R.id.tv_medicineresult_dosage);            // 용법 및 용량
        tvDosage.setOnClickListener(view -> vpResult.setCurrentItem(1));

        TextView tvPrecaution = findViewById(R.id.tv_medicineresult_precaution);    // 주의사항
        tvPrecaution.setOnClickListener(view -> vpResult.setCurrentItem(2));

        TextView tvAppearance = findViewById(R.id.tv_medicineresult_appearance);    // 외형
        tvAppearance.setOnClickListener(view -> vpResult.setCurrentItem(3));

        TextView tvIngredient = findViewById(R.id.tv_medicineresult_ingredient);    // 성분
        tvIngredient.setOnClickListener(view -> vpResult.setCurrentItem(4));

        TextView tvSave = findViewById(R.id.tv_medicineresult_save);                // 저장 방법
        tvSave.setOnClickListener(view -> vpResult.setCurrentItem(5));

        categories = new HashMap<Integer,View>(){
            {
                put(0, tvEfficacy);
                put(1, tvDosage);
                put(2, tvPrecaution);
                put(3, tvAppearance);
                put(4, tvIngredient);
                put(5, tvSave);
            }
        };
    }

    private void setIcon() {
        // 즐겨찾기 여부 설정
        if (medicine.isFavoritesNull()) {
            ibtStar.setImageResource(R.drawable.icon_star_off);
        }
        else {
            ibtStar.setImageResource(R.drawable.icon_star_on);
        }

        // 알림 여부 설정
        if (medicine.isAlarmNull()) {
            ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_off));
            ivAlarm.setTag("off");
        }
        else {
            ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_on));
            ivAlarm.setTag("on");
        }
    }

    private void getMedicineResult() {
        Callback<String> medicineCallback = new Callback<String>() {
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

                        tts.speak("검색된 의약품은 " + name + "입니다.", QUEUE_FLUSH, null, null);

                        JSONObject appearance = data.getJSONObject("appearance");
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

                        Long favoritesIdx = data.isNull("favoritesIdx") ? null : data.getLong("favoritesIdx");

                        AlarmInfo alarmInfo = data.isNull("alarmResponse") ? null : new AlarmInfo(data.getJSONObject("alarmResponse"));

                        medicine = new MedicineInfo(data.getInt("medicineIdx"), name,
                                data.getString("efficacy"), data.getString("dosage"),
                                data.getString("precaution"), appearanceInfo,
                                data.getString("ingredient"), data.getString("save"), favoritesIdx, alarmInfo);

                        tvTitle.setText(medicine.getName());

                        // 즐겨찾기, 알림 여부 설정
                        setIcon();

                        medicinePagerAdapter.setMedicineInfo(medicine);
                        vpResult.setCurrentItem(0);
                        medicinePagerAdapter.notifyItemChanged(0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    try {
                        assert response.errorBody() != null;
                        String errorStr = response.errorBody().string();
                        JSONObject errorBody = new JSONObject(errorStr);
                        long errorIdx = errorBody.getLong("errorIdx");

                        if (errorIdx == RESPONSE_BARCODE_FORMAT_ERROR) {
                            Log.e("api-response", "barcode format error");
                            tts.speak("바코드에 대한 의약품 정보가 없습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                        } else {
                            Log.e("api-response", "bad parameter");
                            tts.speak("서비스 오류로 인해 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 404) {
                    try {
                        assert response.errorBody() != null;
                        String errorStr = response.errorBody().string();
                        JSONObject errorBody = new JSONObject(errorStr);
                        long errorIdx = errorBody.getLong("errorIdx");

                        if (errorIdx == RESPONSE_BARCODE_NOT_FOUND) {
                            Log.e("api-response", "barcode not found");
                            tts.speak("바코드에 대한 의약품 정보가 없습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                        } else if (errorIdx == RESPONSE_MEDICINE_NOT_FOUND) {
                            Log.e("api-response", "medicine not found");
                            tts.speak("해당 의약품은 존재하지 않습니다.", QUEUE_FLUSH, null, null);
                            tts.playSilentUtterance(5000, QUEUE_ADD, null);   // 2초 딜레이
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("api-response", "service error");
                    tts.speak("음성 결과 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("api-response", "no service");
                tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
            }
        };

        if (medicineIdx != 0) {                 // 의약품 번호로 정보 조회
            PillaroidAPIImplementation.getApiService().getMedicineByIdx(SharedPrefManager.read("token", null), medicineIdx).enqueue(medicineCallback);
        } else if (!barcode.equals("")) {       // 바코드로 정보 조회
            PillaroidAPIImplementation.getApiService().getMedicineByBarcode(SharedPrefManager.read("token", null), barcode).enqueue(medicineCallback);
        }
    }
}
