package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;

import com.nadoyagsa.pillaroid.data.MedicineInfo;

import java.util.Locale;
import java.util.Objects;

public class MedicineResultActivity extends AppCompatActivity {
    private MedicineInfo result;

    private TextToSpeech tts;

    private AppCompatImageButton ivAlarm;
    private View dialogView;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_result);

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
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        dialogView = getLayoutInflater().inflate(R.layout.dialog_add_alarm, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //임시 데이터
        result = new MedicineInfo(
                199303108L,
                "타이레놀정500밀리그람",
                "1.주효능·효과\n감기로인한발열및동통(통증),두통,신경통,근육통,월경통,염좌통(삔통증)\n2.다음질환에도사용할수있다.\n치통,관절통,류마티양동통(통증)",
                "만12세이상소아및성인:\n1회1~2정씩1일3-4회(4-6시간마다)필요시복용한다.\n1일최대4그램(8정)을초과하여복용하지않는다.\n이약은가능한최단기간동안최소유효용량으로복용한다.",
                "1.다음과같은사람은이약을복용하지말것\n1)이약에과민증환자\n2)소화성궤양환자\n3)심한혈액이상환자\n4)심한간장애환자\n5)심한신장(콩팥)장애환자\n6)심한심장기능저하환자\n7)아스피린천식(비스테로이드성소염(항염)제에의한천식발작유발)또는그병력이있는환자\n8)다음의약물을복용한환자:바르비탈계약물,삼환계항우울제\n9)알코올을복용한사람\n2.다음과같은사람은이약을복용하기전에의사,치과의사,약사와상의할것\n1)간장애또는그병력이있는환자\n2)신장(콩팥)장애또는그병력이있는환자\n2)신장(콩팥)장애또는그병력이있는환자\n3)소화성궤양의병력이있는환자\n4)혈액이상또는그병력이있는환자\n5)출혈경향이있는환자(혈소판기능이상이나타날수있다.)\n6)심장기능이상이있는환자\n7)과민증의병력이있는환자\n8)기관지천식환자\n9)고령자(노인)\n10)임부또는수유부\n11)와파린을장기복용하는환자\n12)다음의약물을복용한환자:리튬,치아짓계이뇨제",
                "밀폐용기, 실온보관(1-30℃)");
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_medicineresult_icon);
        ivIcon.setImageResource(R.drawable.icon_info);
        ivIcon.setContentDescription("정보안내 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_medicineresult_title);
        tvTitle.setText("타이레놀정500밀리그람");
        tvTitle.setSelected(true);  //ellipsize="marquee" 실행되도록 selected 설정

        AppCompatImageButton ibtStar = toolbar.findViewById(R.id.ibt_ab_medicineresult_star);
        ibtStar.setOnClickListener(v -> {
            //TODO: 즐겨찾기 관리 (ibtStar.tag=on/off)
        });

        ivAlarm = toolbar.findViewById(R.id.ibt_ab_medicineresult_alarm);   //TODO: 시각장애인 모드일 때 description 읽어주는지 확인하기
        ivAlarm.setOnClickListener(v -> showAlarmDialog());
    }

    public final void showAlarmDialog() {
        dialog.show();

        final EditText etLabel = dialogView.findViewById(R.id.et_dialog_addalarm_label);
        etLabel.setHint(result.getMedicineName());

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
                ((TextView)v).setText("취소");
            }
            etLabel.setText("");
            ((EditText) dialogView.findViewById(R.id.et_dialog_addalarm_days)).setText("");
            dialog.dismiss();
        });
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
