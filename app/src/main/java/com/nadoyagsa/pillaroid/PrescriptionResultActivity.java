package com.nadoyagsa.pillaroid;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.nadoyagsa.pillaroid.adapter.PrescriptionPagerAdapter;
import com.nadoyagsa.pillaroid.data.PrescriptionInfo;

import java.util.ArrayList;
import java.util.Objects;

public class PrescriptionResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_result);

        Toolbar toolbar = findViewById(R.id.tb_prescription_result_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_prescription, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        //TODO: 임시 데이터 삭제 바람
        ArrayList<PrescriptionInfo> prescriptionInfos = new ArrayList<>();
        prescriptionInfos.add(new PrescriptionInfo("타이레놀정 160mg", false, "장방형", "앞면은 분할선 없음, 뒷면은 ‘-’형", "나정",
                "해열, 진통, 소염제", "감기로 인한 발열 및 동통(통증), 두통, 신경통, 근육통, 월경통, 염좌통(삔 통증)", "치통, 관절통, 류마티양 동통(통증)"));

        ViewPager2 vpResult = findViewById(R.id.vp_prescription_result);
        PrescriptionPagerAdapter prescriptionPagerAdapter = new PrescriptionPagerAdapter(prescriptionInfos);
        vpResult.setAdapter(prescriptionPagerAdapter);
    }
}
