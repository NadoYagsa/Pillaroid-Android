package com.nadoyagsa.pillaroid;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.tb_main_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_main, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        LinearLayout llCamera = findViewById(R.id.ll_main_camera);
        llCamera.setOnClickListener(v -> startActivity(new Intent(this, SearchCameraActivity.class)));

        LinearLayout llVoice = findViewById(R.id.ll_main_voice);
        llVoice.setOnClickListener(v -> startActivity(new Intent(this, SearchVoiceActivity.class)));

        LinearLayout llPrescription = findViewById(R.id.ll_main_prescription);
        llPrescription.setOnClickListener(v -> startActivity(new Intent(this, SearchPrescriptionActivity.class)));

        LinearLayout llMypage = findViewById(R.id.ll_main_mypage);
        llMypage.setOnClickListener(v -> startActivity(new Intent(this, MypageActivity.class)));
    }
}
