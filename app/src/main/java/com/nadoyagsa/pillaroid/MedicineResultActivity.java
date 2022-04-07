package com.nadoyagsa.pillaroid;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MedicineResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

        Toolbar toolbar = findViewById(R.id.tb_medicineresult_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_medicine_result, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_medicineresult_icon);
        ivIcon.setImageResource(R.drawable.icon_info);
        ivIcon.setContentDescription("정보안내 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_medicineresult_title);
        tvTitle.setText("타이레놀정 160mg");
        tvTitle.setSelected(true);
    }
}
