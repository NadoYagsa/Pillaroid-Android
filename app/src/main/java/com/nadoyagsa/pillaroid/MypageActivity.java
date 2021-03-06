package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import java.util.Locale;
import java.util.Objects;

public class MypageActivity extends AppCompatActivity {
    private boolean isToken = false;

    private AudioManager mAudioManager;
    private TextToSpeech tts;

    private IndicatorSeekBar isbVoiceSpeed;
    private LinearLayout llLogout;
    private TextView tvGuideVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        String token = SharedPrefManager.read("token", "");
        if (!token.equals(""))
            isToken = true;

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

        Toolbar toolbar = findViewById(R.id.tb_mypage_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_mypage, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        llLogout = toolbar.findViewById(R.id.ll_ab_mypage_logout);
        setToolbarListener(llLogout);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        tvGuideVolume = findViewById(R.id.tv_mypage_guide_volume);
        tvGuideVolume.setText(String.valueOf(SharedPrefManager.read("guideVolume", 8)));
        isbVoiceSpeed = findViewById(R.id.isb_mypage_voice_speed);
        isbVoiceSpeed.setProgress(SharedPrefManager.read("voiceSpeed", (float) 1) * 100);

        setListener();
    }

    private void setToolbarListener(LinearLayout llLogout) {
        if (isToken)
            llLogout.setVisibility(View.VISIBLE);
        else
            llLogout.setVisibility(View.INVISIBLE);

        llLogout.setOnClickListener(view -> {
            SharedPrefManager.remove("token");

            isToken = false;
            llLogout.setVisibility(View.INVISIBLE);

            tts.speak("??????????????? ?????????????????????.", QUEUE_FLUSH, null, null);
        });
    }

    private void setListener() {
        @SuppressLint("ApplySharedPref")
        View.OnClickListener guideVolumeClickListener = ivGuideView -> {
            tvGuideVolume = findViewById(R.id.tv_mypage_guide_volume);
            int guideVolume = Integer.parseInt(tvGuideVolume.getText().toString());

            if (guideVolume == mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) && ivGuideView == findViewById(R.id.iv_mypage_guide_plus)) {
                tts.speak("?????? ???????????????.", QUEUE_FLUSH, null, null);
                return;
            } else if (guideVolume == 0 && ivGuideView == findViewById(R.id.iv_mypage_guide_minus)) {
                return;
            }

            if (ivGuideView == findViewById(R.id.iv_mypage_guide_plus)) guideVolume++;
            else if (ivGuideView == findViewById(R.id.iv_mypage_guide_minus)) guideVolume--;

            SharedPrefManager.write("guideVolume", guideVolume);
            tvGuideVolume.setText(String.valueOf(guideVolume));
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, guideVolume, AudioManager.FLAG_SHOW_UI);

            if (guideVolume == 1 || guideVolume == 2 || guideVolume == 4 || guideVolume == 5 || guideVolume == 7 || guideVolume == 8 ||
                    guideVolume == 9 || guideVolume == 11 || guideVolume == 12 || guideVolume == 14 || guideVolume == 15) {
                tts.speak("????????? " + guideVolume + "??? ?????????????????????.", QUEUE_FLUSH, null, null);
            } else if (guideVolume == 0 || guideVolume == 3 || guideVolume == 6 || guideVolume == 10 || guideVolume == 13) {
                tts.speak("????????? " + guideVolume + "?????? ?????????????????????.", QUEUE_FLUSH, null, null);
            } else {
                Log.e("guideVolume", "out of range");
            }
        };
        ImageView ivGuidePlus = findViewById(R.id.iv_mypage_guide_plus);
        ivGuidePlus.setOnClickListener(guideVolumeClickListener);
        ImageView ivGuideMinus = findViewById(R.id.iv_mypage_guide_minus);
        ivGuideMinus.setOnClickListener(guideVolumeClickListener);

        isbVoiceSpeed.setOnSeekChangeListener(new OnSeekChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onSeeking(SeekParams seekParams) {
                float speed = (float) seekParams.progress / 100;
                SharedPrefManager.write("voiceSpeed", speed);
                tts.setSpeechRate(speed);
                tts.speak(speed + " ???????????? ?????????????????????", QUEUE_FLUSH, null, null);
            }
            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) { }
        });

        //????????? ?????? ????????? ???????????? ???(???????????? ????????? ??????)
        ActivityResultLauncher<Intent> startActivityResultLogin = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        isToken = true;
                        llLogout.setVisibility(View.VISIBLE);
                    }
                });

        // ??????????????? ???->MypageFavoritesActivity(???????????? ??????)?????? ??????  ||  ??????????????? ??????->LoginActivity??? ??????
        RelativeLayout rlFavorites = findViewById(R.id.rl_mypage_favorites);
        rlFavorites.setOnClickListener(view -> {
            String token = SharedPrefManager.read("token", "");
            if (!token.equals(""))
                startActivity(new Intent(this, MypageFavoritesActivity.class));
            else {
                tts.speak("???????????? ????????? ???????????? ???????????????. ???????????? ???????????? ?????? ????????? ????????? ????????? ????????? ???????????????.", QUEUE_FLUSH, null, null);

                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("from", 'f');
                startActivityResultLogin.launch(loginIntent);
            }
        });

        // ??????????????? ???->MypageAlarmActivity(?????? ??????)?????? ??????  ||  ??????????????? ??????->LoginActivity??? ??????
        RelativeLayout rlAlarm = findViewById(R.id.rl_mypage_alarm);
        rlAlarm.setOnClickListener(view -> {
            String token = SharedPrefManager.read("token", "");
            if (!token.equals(""))
                startActivity(new Intent(this, MypageAlarmActivity.class));
            else {
                tts.speak("?????? ????????? ???????????? ???????????????. ???????????? ???????????? ?????? ????????? ????????? ????????? ????????? ???????????????.", QUEUE_FLUSH, null, null);
                
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("from", 'a');
                startActivityResultLogin.launch(loginIntent);
            }
        });
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
