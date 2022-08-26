package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicineResultActivity extends AppCompatActivity {
    private final long RESPONSE_BARCODE_FORMAT_ERROR = 40003L;
    private final long RESPONSE_BARCODE_NOT_FOUND = 40401L;
    private final long RESPONSE_MEDICINE_NOT_FOUND = 40402L;
    private final long RESPONSE_DATA_NOT_FOUND = 40403L;
    private final String API_FAILED = "api-failed";

    private int medicineIdx = 0;
    private String barcode = "";
    private MedicineInfo medicine;
    private HashMap<Integer,View> categories;

    private ActivityResultLauncher<Intent> startActivityResultLogin;
    private AlertDialog dialog;
    private AppCompatImageButton ivAlarm, ibtStar;
    private MedicinePagerAdapter medicinePagerAdapter;
    private TextToSpeech tts;
    private TextView tvTitle;
    private View dialogView, selectedCategoryView;
    private ViewPager2 vpResult;

    //TODO: 3. 알람

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

        if (getIntent().hasExtra("medicineIdx"))
            medicineIdx = getIntent().getIntExtra("medicineIdx", 0);   //검색 의약품 idx
        else if (getIntent().hasExtra("barcode"))
            barcode = getIntent().getStringExtra("barcode");    //검색할 바코드

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
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                getMedicineResult();
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

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

        medicine = new MedicineInfo(-1, "", "", "", "", null, "", "", null);
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
            if (result.getResultCode() == RESULT_OK) {
                PillaroidAPIImplementation.getApiService().getFavorites(SharedPrefManager.read("token", ""), medicine.getMedicineIdx()).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.code() == 200) {
                            try {
                                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                JSONObject data = responseJson.getJSONObject("data");

                                medicine.setFavoritesIdx(data.getLong("favoritesIdx"));
                                tts.speak("로그인 되셨습니다. 이후부터 즐겨찾기 추가가 가능합니다.", TextToSpeech.QUEUE_FLUSH, null, null);

                                setFavoritesIcon();     // 즐겨찾기 아이콘 설정
                            } catch (JSONException e) { e.printStackTrace(); }
                        }
                        else if (response.code() == 401) {
                            tts.speak("허가받지 않은 회원의 접근입니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                        }
                        else if (response.code() == 404) {
                            if (response.errorBody() != null) {
                                try {
                                    String errorStr = response.errorBody().string();
                                    JSONObject errorBody = new JSONObject(errorStr);
                                    long errorIdx = errorBody.getLong("errorIdx");

                                    if (errorIdx == RESPONSE_DATA_NOT_FOUND) {  // 이전에 즐겨찾기로 추가하지 않음
                                        tts.speak("로그인 되셨습니다. 이후부터 즐겨찾기 추가가 가능합니다.", TextToSpeech.QUEUE_FLUSH, null, null);

                                        medicine.setFavoritesIdx(null);
                                        setFavoritesIcon();     // 즐겨찾기 아이콘 설정
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                                tts.speak("즐겨찾기 여부 조회에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                        }
                        else {
                            tts.speak("즐겨찾기 여부 조회에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                    }
                });
            }
            else {
                tts.speak("로그인에 문제가 발생해 즐겨찾기 기능 사용이 불가합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
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
                            tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
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
                            tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                        }
                    });
                }
            }
        });

        // 알람
        ivAlarm = toolbar.findViewById(R.id.ibt_ab_medicineresult_alarm);   //TODO: 시각장애인 모드일 때 description 읽어주는지 확인하기
        ivAlarm.setOnClickListener(v -> showAlarmDialog());
    }

    public final void showAlarmDialog() {
        dialog.show();

        final EditText etLabel = dialogView.findViewById(R.id.et_dialog_addalarm_label);
        etLabel.setHint(medicine.getName());

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
                ((TextView) v).setText("취소");
            }
            etLabel.setText("");
            ((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).setText("");
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

    private void setFavoritesIcon() {
        // 즐겨찾기 여부 설정
        if (medicine.isFavoritesNull()) {
            ibtStar.setImageResource(R.drawable.icon_star_off);
        }
        else {
            ibtStar.setImageResource(R.drawable.icon_star_on);
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

                        medicine = new MedicineInfo(data.getInt("medicineIdx"), name,
                                data.getString("efficacy"), data.getString("dosage"),
                                data.getString("precaution"), appearanceInfo,
                                data.getString("ingredient"), data.getString("save"), favoritesIdx);

                        tvTitle.setText(medicine.getName());

                        // 즐겨찾기 여부 설정
                        setFavoritesIcon();

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
                            tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);   // 2초 딜레이
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
