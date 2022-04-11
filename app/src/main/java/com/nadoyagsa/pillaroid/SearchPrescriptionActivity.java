package com.nadoyagsa.pillaroid;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SearchPrescriptionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_prescription);

        /* TODO: 처방전 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, PrescriptionResultActivity.class));
    }
}
