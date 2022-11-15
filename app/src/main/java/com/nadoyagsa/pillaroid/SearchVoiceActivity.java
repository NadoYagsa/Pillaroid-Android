package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Objects;

public class SearchVoiceActivity extends AppCompatActivity {
    private boolean isRecording = false;        // 볼륨 버튼을 위함
    private boolean isResultEnd = false;        // 음성 결과 반환 여부 확인 (음성 결과를 반환하기 전에 인식을 종료함 방지)
    private boolean isResultBtClicked = false;  // 결과 확인하기 버튼이 클릭되었는지 확인 (isResultEnd 시 넘어감 방지)
    private String temporaryQuery = "";

    private SoundPool soundPool;
    private int soundID;

    private EditText etQuery;
    private Intent intent;
    private RecognitionListener recognitionListener;
    private SpeechRecognizer speechRecognizer;

    private long delay = 0;
    private View currentClickedView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_voice);

        checkRecordPermission();    // 음성 인식을 위한 녹음 퍼미션 체크

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);	//작성
        soundID = soundPool.load(this,R.raw.ding_dong,1);

        Toolbar toolbar = findViewById(R.id.tb_voicesearch_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_icon_text, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        initActionBar(toolbar);

        etQuery = findViewById(R.id.et_voicesearch_query);
        settingForSTT();
        setClickListener();
    }

    private void speakRecordMethod() {
        tts.speak(getString(R.string.text_search_voice_guide1), TextToSpeech.QUEUE_FLUSH, null, null);
        tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null);
        tts.speak(getString(R.string.text_search_voice_guide2), TextToSpeech.QUEUE_ADD, null, null);
        tts.speak(getString(R.string.text_search_voice_guide3), TextToSpeech.QUEUE_ADD, null, null);
        tts.speak(getString(R.string.text_search_voice_guide4), TextToSpeech.QUEUE_ADD, null, null);
    }

    private void checkRecordPermission() {
        //TODO: layout text들 string.xml에 넣으면 getString(R.string._)로 바꾸기
        if (Build.VERSION.SDK_INT >= 23) {
            // 녹음 권한이 없으면 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_DENIED) {
                tts.speak("Audio recording permission is required for voice recognition.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("Click the Allow button on the far right of the center of the screen.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("If permission is denied, return to the main screen.", TextToSpeech.QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
            }
            else
                speakRecordMethod();
        }
        else {
            tts.speak("Speech recognition is not possible due to the low SDK version.", TextToSpeech.QUEUE_ADD, null, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1000 && grantResults.length>0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                speakRecordMethod();
            else
                finish();
        }
    }

    private void initActionBar(Toolbar toolbar) {
        ImageView ivIcon = toolbar.findViewById(R.id.iv_ab_icontext_icon);
        ivIcon.setImageResource(R.drawable.icon_voice);
        ivIcon.setContentDescription("Voice Recorder Icon");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText(getString(R.string.page_search_voice));
    }

    private void settingForSTT() {
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {}
            @Override
            public void onBeginningOfSpeech() {
                isResultEnd = false;    // 결과 반환이 아직 되지 않았음
            }
            @Override
            public void onRmsChanged(float v) {}
            @Override
            public void onBufferReceived(byte[] bytes) {}
            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {    // 음성 인식 오류 발생 시
                String message;

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        return;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "No permission";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                        message = "no language support";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                        message = "language not used";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        if (isRecording)
                            startRecord();
                        return;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "recognition error";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "recording timeout";
                        break;
                    case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                        message = "server connection error";
                        break;
                    case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                        message = "too many requests";
                        break;
                    default:
                        message = "unknown error";
                        break;
                }
                speechRecognizer.cancel();
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            }

            @Override
            public void onResults(Bundle bundle) {
                isResultEnd = true;

                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                // 인식 결과
                StringBuilder newText = new StringBuilder();
                for (int i=0; i<matches.size() ; i++) {
                    newText.append(matches.get(i));
                }

                temporaryQuery = temporaryQuery.concat(newText.toString());
                etQuery.setText(temporaryQuery);

                if (isRecording)    // 인식 종료 버튼이 아직 눌리지 않음 (녹음 재개)
                    speechRecognizer.startListening(intent);
                else {              // 인식 종료 버튼이 눌렸을 때, 종료 시점 이후에 결과가 반환이 되는 경우
                    temporaryQuery = "";
                    etQuery.setText(etQuery.getText().toString().replaceAll("\\s", ""));
                    tts.speak("End of speech recognition." + etQuery.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                }

                if (isResultBtClicked) {
                    isResultBtClicked = false;

                    Intent resultIntent = new Intent(SearchVoiceActivity.this, VoiceResultsActivity.class);
                    resultIntent.putExtra("query", etQuery.getText());
                    startActivity(resultIntent);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {}
            @Override
            public void onEvent(int i, Bundle bundle) {}
        };
    }

    // 음성 인식 시작
    private void startRecord() {
        isRecording = true;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(recognitionListener);
        speechRecognizer.startListening(intent);
    }
    // 음성 인식 종료
    private void endRecord() {
        isRecording = false;

        speechRecognizer.stopListening();

        // 인식 결과가 모두 출력되었을 경우
        if (isResultEnd) {
            temporaryQuery = "";
            etQuery.setText(etQuery.getText().toString().replaceAll("\\s", ""));
            tts.speak("End of speech recognition." + etQuery.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 음성 인식 시작 및 종료
                if (isRecording)
                    endRecord();
                else {
                    tts.stop();
                    soundPool.play(soundID,1f,1f,0,0,1f);	//작성
                    startRecord();
                }
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {    // 음성 인식 취소
                if (isRecording) {
                    speechRecognizer.cancel();

                    isRecording = false;
                    temporaryQuery = "";
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void setClickListener() {
        AppCompatButton btResult = findViewById(R.id.bt_voicesearch_result);
        btResult.setOnClickListener(v -> {
            if (System.currentTimeMillis() > delay) {
                currentClickedView = v;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("Button." + ((AppCompatButton) v).getText(), QUEUE_FLUSH, null, null);
            } else if (currentClickedView == v) {
                tts.stop();     //진행중이던 tts speak가 있다면 멈춤
                if (etQuery.getText().length() > 0) {
                    isResultBtClicked = true;

                    if (isRecording)
                        speechRecognizer.cancel();

                    if (isResultEnd) {
                        isResultBtClicked = false;

                        Intent resultIntent = new Intent(this, VoiceResultsActivity.class);
                        resultIntent.putExtra("query", etQuery.getText().toString());
                        startActivity(resultIntent);
                    }
                    // isResultEnd == false일 때는 recognitionListener에서 처리함
                } else
                    tts.speak("There are no words to search for.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        TextView tvDescriptionResult = findViewById(R.id.tv_voicesearch_description_result);
        tvDescriptionResult.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule = findViewById(R.id.tv_voicesearch_description_rule);
        tvDescriptionRule.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule1 = findViewById(R.id.tv_voicesearch_description_rule1);
        tvDescriptionRule1.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text. " + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule2 = findViewById(R.id.tv_voicesearch_description_rule2);
        tvDescriptionRule2.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule3 = findViewById(R.id.tv_voicesearch_description_rule3);
        tvDescriptionRule3.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("Text." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });
    }
}
