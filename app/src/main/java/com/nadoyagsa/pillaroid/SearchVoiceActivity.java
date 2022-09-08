package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        tts.speak("검색할 의약품 이름을 볼륨 버튼을 누르고 말해주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
        tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null);
        tts.speak("녹음 시작 시 '띵똥' 소리가 들립니다.", TextToSpeech.QUEUE_ADD, null, null);
        tts.speak("말하기를 완료하셨다면 볼륨 버튼을 다시 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
        tts.speak("재녹음도 동일한 방법으로 진행됩니다.", TextToSpeech.QUEUE_ADD, null, null);
    }

    private void checkRecordPermission() {
        //TODO: layout text들 string.xml에 넣으면 getString(R.string._)로 바꾸기
        if (Build.VERSION.SDK_INT >= 23) {
            // 녹음 권한이 없으면 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_DENIED) {
                tts.speak("음성인식을 위해 오디오녹음 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("권한 거부 시에는 메인 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
            }
            else
                speakRecordMethod();
        }
        else {
            tts.speak("SDK 버전이 낮아 음성 인식이 불가합니다.", TextToSpeech.QUEUE_ADD, null, null);
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
        ivIcon.setContentDescription("녹음기 아이콘");

        TextView tvTitle = toolbar.findViewById(R.id.tv_ab_icontext_title);
        tvTitle.setText("의약품 음성으로 검색");
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
                        message = "오디오 에러";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        return;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "권한 없음";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                        message = "언어 지원 안함";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                        message = "언어 사용 안됨";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 오류";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "네트워크 시간 초과";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        // 녹음을 오래하거나 speechRecognizer.stopListening()을 호출하면 발생하는 오류
                        if (isRecording)
                            startRecord();
                        return;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "인식 오류";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "서버 오류";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "녹음 시간 초과";
                        break;
                    case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                        message = "서버 연결 오류";
                        break;
                    case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                        message = "요청 과다";
                        break;
                    default:
                        message = "알 수 없는 오류";
                        break;
                }
                speechRecognizer.cancel();
                tts.speak(message.concat(" 문제가 발생하였습니다."), TextToSpeech.QUEUE_FLUSH, null, null);
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
                    tts.speak("음성 인식 종료. " + etQuery.getText(), TextToSpeech.QUEUE_FLUSH, null, null);
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
            tts.speak("음성 인식 종료", TextToSpeech.QUEUE_FLUSH, null, null);
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
                tts.speak("버튼." + ((AppCompatButton) v).getText(), QUEUE_FLUSH, null, null);
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
                    tts.speak("검색할 단어가 없습니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        TextView tvDescriptionResult = findViewById(R.id.tv_voicesearch_description_result);
        tvDescriptionResult.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule = findViewById(R.id.tv_voicesearch_description_rule);
        tvDescriptionRule.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule1 = findViewById(R.id.tv_voicesearch_description_rule1);
        tvDescriptionRule1.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트. " + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule2 = findViewById(R.id.tv_voicesearch_description_rule2);
        tvDescriptionRule2.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });

        TextView tvDescriptionRule3 = findViewById(R.id.tv_voicesearch_description_rule3);
        tvDescriptionRule3.setOnClickListener(v -> {
            currentClickedView = v;
            tts.speak("텍스트." + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
        });
    }
}
