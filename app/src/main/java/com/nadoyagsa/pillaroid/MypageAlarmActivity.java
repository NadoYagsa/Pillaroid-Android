package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.AlarmRecyclerAdapter;
import com.nadoyagsa.pillaroid.adapter.ItemTouchHelperCallback;
import com.nadoyagsa.pillaroid.data.AlarmInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageAlarmActivity extends AppCompatActivity {

    private ArrayList<AlarmInfo> alarmList;
    private AlarmRecyclerAdapter alarmAdapter;
    private TextView tvAlarmInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_alarm);

        alarmList = new ArrayList<>();

        Toolbar toolbar = findViewById(R.id.tb_alarm_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_mypage_alarm, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        toolbarListener(toolbar);

        RecyclerView rvAlarm = findViewById(R.id.rv_alarm_list);
        LinearLayoutManager alarmManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        rvAlarm.setLayoutManager(alarmManager);
        alarmAdapter = new AlarmRecyclerAdapter(alarmList);
        rvAlarm.setAdapter(alarmAdapter);
        DividerItemDecoration devider = new DividerItemDecoration(this, 1);
        devider.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divide_bar, null)));
        rvAlarm.addItemDecoration(devider);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelperCallback(alarmAdapter));
        helper.attachToRecyclerView(rvAlarm);

        tvAlarmInfo = findViewById(R.id.tv_alarm_info);

        getAlarmList();
    }

    private void toolbarListener(Toolbar toolbar) {
        AppCompatButton btSetTime = toolbar.findViewById(R.id.bt_ab_alarm_time_setting);
        btSetTime.setOnClickListener(view -> startActivity(new Intent(this, TimeSettingActivity.class)));
    }

    private void getAlarmList() {
        tvAlarmInfo.setVisibility(View.VISIBLE);
        tvAlarmInfo.setText(getString(R.string.text_alarm_search));

        alarmList.clear();
        alarmAdapter.notifyDataSetChanged();
        PillaroidAPIImplementation.getApiService().getAlarmList(SharedPrefManager.read("token", "")).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    JSONObject responseJson = null;
                    try {
                        responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                        JSONArray results = responseJson.getJSONArray("data");

                        if (results.length() == 0) {
                            tts.speak("There are no medications for which notifications have been set.", QUEUE_FLUSH, null, null);
                            tvAlarmInfo.setText(getString(R.string.text_alarm_no_result));
                        } else {
                            tvAlarmInfo.setVisibility(View.GONE);

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject alarms = results.getJSONObject(i);
                                alarmList.add(new AlarmInfo(alarms));
                            }
                            alarmAdapter.notifyDataSetChanged();

                            tts.speak("There are a total of " +  alarmList.size() + " notification lists.", QUEUE_FLUSH, null, null);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 401) {
                    tts.speak("Access by unauthorized members. Return to the previous screen.", QUEUE_FLUSH, null, null);
                    finish();
                } else {
                    tts.speak("There was a problem with the notification list lookup. Return to the previous screen.", QUEUE_FLUSH, null, null);
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
}
