package com.nadoyagsa.pillaroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SearchCaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_case);

        /* TODO: 용기를 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, MedicineResultActivity.class));
    }
}
