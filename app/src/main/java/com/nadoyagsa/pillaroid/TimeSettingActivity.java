package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimeSettingActivity extends AppCompatActivity {
    private EditText activatedEditText = null;
    private EditText etTimeMorningHour;
    private EditText etTimeMorningMinute;
    private EditText etTimeLunchHour;
    private EditText etTimeLunchMinute;
    private EditText etTimeDinnerHour;
    private EditText etTimeDinnerMinute;

    private long delay = 0;
    private View currentClickedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_setting);

        Toolbar toolbar = findViewById(R.id.tb_time_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        /* 액션바 아이콘과 주제 설정 */
        ImageView ivIcon = customView.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_alarm);
        TextView tvTopic = customView.findViewById(R.id.tv_ab_icontext_title);
        tvTopic.setText("알림 시간대 설정");
        tvTopic.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        etTimeMorningHour = findViewById(R.id.et_time_morning_hour);
        etTimeMorningMinute = findViewById(R.id.et_time_morning_minute);
        etTimeLunchHour = findViewById(R.id.et_time_lunch_hour);
        etTimeLunchMinute = findViewById(R.id.et_time_lunch_minute);
        etTimeDinnerHour = findViewById(R.id.et_time_dinner_hour);
        etTimeDinnerMinute = findViewById(R.id.et_time_dinner_minute);

        tts.speak("Please set a time of day to eat most of the time. You will be reminded to take your medicine according to the set time zone.", QUEUE_FLUSH, null, null);
        tts.speak("Press the time zone to activate it and adjust the value with the volume buttons.", QUEUE_ADD, null, null);

        requestMealTime();
        setListener();
    }

    // 사용자의 알림 시간대 조회
    private void requestMealTime() {
        PillaroidAPIImplementation.getApiService().getMealTime(SharedPrefManager.read("token", "")).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    try {
                        JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                        JSONObject result = responseJson.getJSONObject("data");

                        // 각각 파싱해서 setText
                        String[] mornings = result.getString("morning").split(":");
                        String[] lunches = result.getString("lunch").split(":");
                        String[] dinners = result.getString("dinner").split(":");

                        etTimeMorningHour.setText(mornings[0]);
                        etTimeMorningMinute.setText(mornings[1]);
                        etTimeLunchHour.setText(lunches[0]);
                        etTimeLunchMinute.setText(lunches[1]);
                        etTimeDinnerHour.setText(dinners[0]);
                        etTimeDinnerMinute.setText(dinners[1]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("Can't connect to server. Return to the previous screen.", QUEUE_FLUSH, null, null);
                finish();
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            String[] tags = activatedEditText.getTag().toString().split("_");   // 0: morning/lunch/dinner, 1: hour/minute
            int time = Integer.parseInt(activatedEditText.getText().toString());

            switch (tags[1]) {
                case "hour":
                    if (time >= 23) {
                        tts.speak("0 o'clock", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(R.string.text_time_setting_00);
                    } else {
                        tts.speak(++time + "o'clock", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(String.format("%02d", time));
                    }
                    break;
                case "minute":
                    if (time >= 50) {
                        tts.speak("0 minutes", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(R.string.text_time_setting_00 );
                    } else {
                        time += 10;
                        tts.speak(time + "minutes", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(String.format("%02d", time));
                    }
                    break;
            }
            return true;    // 볼륨 UP 기능 제거
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            String[] tags = activatedEditText.getTag().toString().split("_");   // 0: morning/lunch/dinner, 1: hour/minute
            int time = Integer.parseInt(activatedEditText.getText().toString());

            switch (tags[1]) {
                case "hour":
                    if (time <= 0) {
                        tts.speak("23 o'clock", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(R.string.text_time_setting_hour_last);
                    } else {
                        tts.speak(--time + "o'clock", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(String.format("%02d", time));
                    }
                    break;
                case "minute":
                    if (time <= 0) {
                        tts.speak("50 minutes", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(R.string.text_time_setting_minute_last);
                    } else {
                        time -= 10;
                        tts.speak(time + "minutes", QUEUE_FLUSH, null, null);
                        activatedEditText.setText(String.format("%02d", time));
                    }
                    break;
            }
            return true;    // 볼륨 DOWN 기능 제거
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("DefaultLocale")
    private void setListener() {
        TextView tvAnnounce = findViewById(R.id.tv_time_announce);
        tvAnnounce.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        TextView tvMorning = findViewById(R.id.tv_time_morning);
        tvMorning.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        TextView tvLunch = findViewById(R.id.tv_time_lunch);
        tvLunch.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        TextView tvAM = findViewById(R.id.tv_time_lunch_am);
        tvAM.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });
        TextView tvPM = findViewById(R.id.tv_time_lunch_pm);
        tvPM.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });

        TextView tvDinner = findViewById(R.id.tv_time_dinner);
        tvDinner.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText().toString(), QUEUE_FLUSH, null, null);
        });

        // time EditText 리스너
        View.OnClickListener etTimeClickListener = view -> {    // 클릭 시 항목 활성화
            activatedEditText = (EditText) view;

            String[] tags = activatedEditText.getTag().toString().split("_");   // 0: morning/lunch/dinner, 1: hour/minute

            String mealType = tags[0];
            String timeType = tags[1].equals("hour") ? "o'clock" : "minutes";

            String time = activatedEditText.getText().toString();
            if (time.equals("")) {  // 값이 없다면 0으로 초기화
                time = "00";
                activatedEditText.setText(R.string.text_time_setting_00);
            }

            tts.speak(String.format("The selected item is %s %s %s.", mealType, time, timeType), QUEUE_FLUSH, null, null);
        };

        etTimeMorningHour.setOnClickListener(etTimeClickListener);
        etTimeMorningMinute.setOnClickListener(etTimeClickListener);
        etTimeLunchHour.setOnClickListener(etTimeClickListener);
        etTimeLunchMinute.setOnClickListener(etTimeClickListener);
        etTimeDinnerHour.setOnClickListener(etTimeClickListener);
        etTimeDinnerMinute.setOnClickListener(etTimeClickListener);


        // 저장하기 버튼
        AppCompatButton btTimeSettingComplete = findViewById(R.id.bt_time_setting_complete);
        btTimeSettingComplete.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("Button." + ((AppCompatButton) v).getText().toString(), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                String morningHour = etTimeMorningHour.getText().toString();
                String morningMinute = etTimeMorningMinute.getText().toString();
                String lunchHour = etTimeLunchHour.getText().toString();
                String lunchMinute = etTimeLunchMinute.getText().toString();
                String dinnerHour = etTimeDinnerHour.getText().toString();
                String dinnerMinute = etTimeDinnerMinute.getText().toString();

                // 데이터 유효성 검사 (빈칸 있으면 X)
                int missingCount = 0;
                StringBuilder missingItems = new StringBuilder();
                if (morningHour.equals("")) {
                    missingItems.append("Breakfast Hour, ");
                    missingCount++;
                }
                if (morningMinute.equals("")) {
                    missingItems.append("Breakfast Minute, ");
                    missingCount++;
                }
                if (lunchHour.equals("")) {
                    missingItems.append("Lunch Hour, ");
                    missingCount++;
                }
                if (lunchMinute.equals("")) {
                    missingItems.append("Lunch Minute, ");
                    missingCount++;
                }
                if (dinnerHour.equals("")) {
                    missingItems.append("Dinner Hour, ");
                    missingCount++;
                }
                if (dinnerMinute.equals("")) {
                    missingItems.append("Dinner Minute, ");
                    missingCount++;
                }
                if (missingCount != 0) {
                    if (missingCount == 1) {
                        tts.speak("Cannot save because " + missingItems + " is not entered.", QUEUE_FLUSH, null, null);
                        return;
                    } else {
                        tts.speak("Cannot save because " + missingItems + " are not entered.", QUEUE_FLUSH, null, null);
                        return;
                    }
                }

                // 사용자 알림 시간대 등록
                JsonObject request = new JsonObject();
                request.addProperty("morning", String.format("%s:%s", morningHour, morningMinute));
                request.addProperty("lunch", String.format("%s:%s", lunchHour, lunchMinute));
                request.addProperty("dinner", String.format("%s:%s", dinnerHour, dinnerMinute));
                PillaroidAPIImplementation.getApiService().postMealTime(SharedPrefManager.read("token", ""), request).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.code() == 200) {
                            try {
                                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                JSONObject result = responseJson.getJSONObject("data");

                                String[] mornings = result.getString("morning").split(":");
                                String[] lunches = result.getString("lunch").split(":");
                                String[] dinners = result.getString("dinner").split(":");

                                String resultText = String.format("Saved as %s:%s in the morning, %s:%s in the afternoon, and %s:%s in the evening",
                                        mornings[0], mornings[1], lunches[0], lunches[1], dinners[0], dinners[1]);
                                tts.speak(resultText, QUEUE_FLUSH, null, null);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (response.code() == 400) {
                            tts.speak("Invalid value. Please reset your time zone.", QUEUE_FLUSH, null, null);
                        } else {
                            tts.speak("There was a problem with the notification time zone setting. Return to the previous screen.", QUEUE_FLUSH, null, null);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        tts.speak("Can't connect to server. Return to the previous screen.", QUEUE_FLUSH, null, null);
                        finish();
                    }
                });
            }
        });
    }
}
