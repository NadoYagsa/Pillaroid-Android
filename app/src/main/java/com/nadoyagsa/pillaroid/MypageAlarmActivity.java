package com.nadoyagsa.pillaroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.AlarmRecyclerAdapter;
import com.nadoyagsa.pillaroid.data.AlarmInfo;

import java.util.ArrayList;
import java.util.Objects;

public class MypageAlarmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_alarm);

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

        ArrayList<AlarmInfo> alarmInfos = new ArrayList<>();
        alarmInfos.add(new AlarmInfo(1L, "타이레놀정 160mg", "1일 2회, 1회 1정, 식후 30분"));
        alarmInfos.add(new AlarmInfo(2L, "인사돌플러스정", "1일 3회, 1회 1정, 식전 30분"));

        RecyclerView rvAlarm = findViewById(R.id.rv_alarm_list);
        LinearLayoutManager alarmManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        rvAlarm.setLayoutManager(alarmManager);
        AlarmRecyclerAdapter alarmAdapter = new AlarmRecyclerAdapter(alarmInfos);
        rvAlarm.setAdapter(alarmAdapter);
        DividerItemDecoration devider = new DividerItemDecoration(this, 1);
        devider.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divide_bar, null)));
        rvAlarm.addItemDecoration(devider);
    }

    private void toolbarListener(Toolbar toolbar) {
        AppCompatButton btSetTime = toolbar.findViewById(R.id.bt_ab_alarm_time_setting);
        btSetTime.setOnClickListener(view -> startActivity(new Intent(this, TimeSettingActivity.class)));
    }
}
