package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

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
    private final String API_FAILED = "api-failed";

    private Long medicineIdx = 0L;
    private String barcode = "";
    private MedicineInfo medicine;
    private HashMap<Integer,View> categories;

    private AlertDialog dialog;
    private AppCompatImageButton ivAlarm;
    private MedicinePagerAdapter medicinePagerAdapter;
    private TextToSpeech tts;
    private TextView tvTitle;
    private View dialogView, selectedCategoryView;
    private ViewPager2 vpResult;

    //TODO: 2. ????????????
    //TODO: 3. ??????

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

        //TODO: if????????? idx, name ???????????????!
        if (getIntent().hasExtra("medicineIdx"))
            medicineIdx = getIntent().getLongExtra("medicineIdx", 0L);   //?????? ????????? idx
        else if (getIntent().hasExtra("barcode"))
            barcode = getIntent().getStringExtra("barcode");    //????????? ?????????

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
                if (utteranceId.equals(API_FAILED)) {   //api ?????? ??? ????????? ????????? ?????? ???????????? ?????????
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

        medicine = new MedicineInfo(-1L, -1L, "", "", "", "", null, "", "");
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
                ((TextView)selectedCategoryView).setTextColor(getColor(R.color.main_color));
                ((TextView)selectedCategoryView).setTypeface(Typeface.DEFAULT_BOLD);

                vpResult.setCurrentItem(position);
                super.onPageSelected(position);
            }
        });
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_medicineresult_icon);
        ivIcon.setImageResource(R.drawable.icon_info);
        ivIcon.setContentDescription("???????????? ?????????");

        tvTitle = toolbar.findViewById(R.id.tv_ab_medicineresult_title);
        tvTitle.setText("");
        tvTitle.setSelected(true);  //ellipsize="marquee" ??????????????? selected ??????

        AppCompatImageButton ibtStar = toolbar.findViewById(R.id.ibt_ab_medicineresult_star);
        ibtStar.setOnClickListener(v -> {
            //TODO: ???????????? ?????? (ibtStar.tag=on/off)
        });

        ivAlarm = toolbar.findViewById(R.id.ibt_ab_medicineresult_alarm);   //TODO: ??????????????? ????????? ??? description ??????????????? ????????????
        ivAlarm.setOnClickListener(v -> showAlarmDialog());
    }

    public final void showAlarmDialog() {
        dialog.show();

        final EditText etLabel = dialogView.findViewById(R.id.et_dialog_addalarm_label);
        etLabel.setHint(medicine.getMedicineName());

        TextView tvCancel = dialogView.findViewById(R.id.tv_dialog_addalarm_cancel);
        if (ivAlarm.getTag().equals("on")) {
            tvCancel.setText("??????");
        }

        TextView tvOk = dialogView.findViewById(R.id.tv_dialog_addalarm_ok);
        tvOk.setOnClickListener(v -> {
            String label = etLabel.getText().toString().equals("") ? etLabel.getHint().toString() : etLabel.getText().toString();
            int days = Integer.parseInt(((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).getText().toString());

            tts.speak(label + " ???????????? ????????? ?????????????????????. ?????? ????????? " + days + "??? ?????????.", QUEUE_FLUSH, null, null);

            //TODO: days ?????? ???????????? ????????? ?????? ?????? ??????

            dialog.hide();
            ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_on));
            ivAlarm.setTag("on");
        });

        tvCancel.setOnClickListener(v -> {
            if (ivAlarm.getTag().equals("on")) {
                tts.speak("????????? ?????????????????????.", QUEUE_FLUSH, null, null);

                //TODO: ????????? ?????? ?????? ?????? (????????? ??? ????????? ?????????????????? ?????? ??????)

                ivAlarm.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.icon_bell_off));
                ivAlarm.setTag("off");
                ((TextView) v).setText("??????");
            }
            etLabel.setText("");
            ((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).setText("");
            dialog.dismiss();
        });
    }

    private void clickListener() {
        TextView tvEfficacy = findViewById(R.id.tv_medicineresult_efficacy);        // ?????? ??? ??????
        tvEfficacy.setOnClickListener(view -> vpResult.setCurrentItem(0));

        TextView tvUsage = findViewById(R.id.tv_medicineresult_usage);              // ?????? ??? ??????
        tvUsage.setOnClickListener(view -> vpResult.setCurrentItem(1));

        TextView tvPrecautions = findViewById(R.id.tv_medicineresult_precautions);   // ????????????
        tvPrecautions.setOnClickListener(view -> vpResult.setCurrentItem(2));

        TextView tvAppearance = findViewById(R.id.tv_medicineresult_appearance);    // ??????
        tvAppearance.setOnClickListener(view -> vpResult.setCurrentItem(3));

        TextView tvIngredient = findViewById(R.id.tv_medicineresult_ingredient);    // ??????
        tvIngredient.setOnClickListener(view -> vpResult.setCurrentItem(4));

        TextView tvSave = findViewById(R.id.tv_medicineresult_save);                // ?????? ??????
        tvSave.setOnClickListener(view -> vpResult.setCurrentItem(5));

        categories = new HashMap<Integer,View>(){
            {
                put(0, tvEfficacy);
                put(1, tvUsage);
                put(2, tvPrecautions);
                put(3, tvAppearance);
                put(4, tvIngredient);
                put(5, tvSave);
            }
        };
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

                        tts.speak("????????? ???????????? " + name + "?????????.", QUEUE_FLUSH, null, null);

                        JSONObject jsonObject = new JSONObject(data.getString("appearanceInfo"));
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

                        medicine = new MedicineInfo(data.getLong("idx"), data.getLong("code"),
                                name, data.getString("efficacy"), data.getString("usage"),
                                data.getString("precautions"), appearanceInfo,
                                data.getString("ingredient"), data.getString("save"));

                        tvTitle.setText(medicine.getMedicineName());

                        medicinePagerAdapter.setMedicineInfo(medicine);
                        vpResult.setCurrentItem(0);
                        medicinePagerAdapter.notifyItemChanged(0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    try {
                        String errorStr = response.errorBody().string();
                        JSONObject errorBody = new JSONObject(errorStr);
                        long errorIdx = errorBody.getLong("errorIdx");

                        if (errorIdx == RESPONSE_BARCODE_FORMAT_ERROR) {
                            Log.e("api-response", "barcode format error");
                            tts.speak("???????????? ?????? ????????? ????????? ????????????. ?????? ???????????? ???????????????.", QUEUE_FLUSH, null, API_FAILED);
                        } else {
                            Log.e("api-response", "bad parameter");
                            tts.speak("????????? ????????? ?????? ?????? ???????????? ???????????????.", QUEUE_FLUSH, null, API_FAILED);
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 404) {
                    try {
                        String errorStr = response.errorBody().string();
                        JSONObject errorBody = new JSONObject(errorStr);
                        long errorIdx = errorBody.getLong("errorIdx");

                        if (errorIdx == RESPONSE_BARCODE_NOT_FOUND) {
                            Log.e("api-response", "barcode not found");
                            tts.speak("???????????? ?????? ????????? ????????? ????????????. ?????? ???????????? ???????????????.", QUEUE_FLUSH, null, API_FAILED);
                        } else if (errorIdx == RESPONSE_MEDICINE_NOT_FOUND) {
                            Log.e("api-response", "medicine not found");
                            tts.speak("?????? ???????????? ???????????? ????????????.", QUEUE_FLUSH, null, null);
                            tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);   // 2??? ?????????
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("api-response", "service error");
                    tts.speak("?????? ?????? ????????? ????????? ???????????????. ?????? ???????????? ???????????????.", QUEUE_FLUSH, null, API_FAILED);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("api-response", "no service");
                tts.speak("????????? ????????? ?????? ????????????. ?????? ???????????? ???????????????.", QUEUE_FLUSH, null, API_FAILED);
            }
        };

        if (!medicineIdx.equals(0L)) {      // ????????? ????????? ?????? ??????
            PillaroidAPIImplementation.getApiService().getMedicineByIdx(medicineIdx).enqueue(medicineCallback);
        } else if (!barcode.equals("")) {      // ???????????? ?????? ??????
            PillaroidAPIImplementation.getApiService().getMedicineByBarcode(barcode).enqueue(medicineCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //tts ?????? ??????
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
