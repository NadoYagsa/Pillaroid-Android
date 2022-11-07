package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchPharmacyEnvelopeActivity  extends AppCompatActivity {
    private static final String PHARMACY_ENVELOPE_LAST_GUIDE = "pharmacy-envelope-last-guide";
    private static final int REQUEST_CODE_PERMISSIONS = 1000;

    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView pvPharmacyEnvelopeCamera;
    private PharmacyEnvelopeAnalyzer pharmacyEnvelopeAnalyzer;
    private TextRecognizer recognizer;

    private boolean canUseCamera = false;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_pharmacy_envelope);

        pvPharmacyEnvelopeCamera = findViewById(R.id.pv_search_pharmacy_envelope);

        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        pharmacyEnvelopeAnalyzer = new PharmacyEnvelopeAnalyzer();
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= 21) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                canUseCamera = true;

                tts.speak("후면 카메라가 켜졌습니다. 약국 봉투를 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
                startCamera();
            } else {
                tts.speak("약국 봉투 인식을 위해 카메라 권한이 필요합니다.", QUEUE_FLUSH, null, null);
                tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", QUEUE_ADD, null, null);
                tts.speak("권한 거부 시에는 메인 화면으로 돌아갑니다.", QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
            }
        } else {
            tts.speak("SDK 버전이 낮아 카메라 사용이 불가합니다.", QUEUE_ADD, null, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                canUseCamera = true;

                tts.speak("후면 카메라가 켜졌습니다. 약국 봉투를 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
                startCamera();
            } else {
                tts.speak("카메라 권한이 승인되지 않아 기능을 사용할 수 없습니다.", QUEUE_FLUSH, null, null);
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();

                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(imageCapture)
                        .build();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);

                preview.setSurfaceProvider(pvPharmacyEnvelopeCamera.getSurfaceProvider());
            } catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 촬영 버튼 클릭
                if (!isSearching) {
                    isSearching = true;
                    tts.speak("사진이 찍혔습니다.", QUEUE_FLUSH, null, null);

                    imageCapture.takePicture(ContextCompat.getMainExecutor(SearchPharmacyEnvelopeActivity.this), new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            super.onCaptureSuccess(image);

                            pharmacyEnvelopeAnalyzer.analyze(image);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            tts.speak("사진을 찍는데 오류가 발생했습니다. 다시 시도해주세요.", QUEUE_FLUSH, null, PHARMACY_ENVELOPE_LAST_GUIDE);
                        }
                    });
                }

                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals(PHARMACY_ENVELOPE_LAST_GUIDE)) {   // 마지막 가이드가 끝나면 사진 분석 가능
                    isSearching = false;
                }
            }

            @Override
            public void onError(String utteranceId) { }
        });

        if (canUseCamera) {
            startCamera();
        } else {
            checkCameraPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cameraProviderFuture != null) {
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
        }
    }

    private class PharmacyEnvelopeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError")
            Image phamacyEnvelopeImage = imageProxy.getImage();
            if (phamacyEnvelopeImage == null) {
                return;
            }

            InputImage image = InputImage.fromMediaImage(phamacyEnvelopeImage, imageProxy.getImageInfo().getRotationDegrees());
            recognizer.process(image)
                    .addOnSuccessListener(new PharmacyEnvelopeSuccessListener())
                    .addOnFailureListener(e -> {
                        tts.speak("약국 봉투의 텍스트를 읽는데 오류가 발생했습니다.", QUEUE_FLUSH, null, PHARMACY_ENVELOPE_LAST_GUIDE);
                    });
        }
    }

    private class PharmacyEnvelopeSuccessListener implements OnSuccessListener<Text> {
        @Override
        public void onSuccess(Text text) {
            // TODO: 주의사항에 있는 단어를 착각할 수 있어 일단 제외시킴
            // 약제 종류 (인식된 단어가 string에 endswith 되는지로 판별)
            /*String[] categories = new String[]{
                    "억제제", "조절제", "치료제", "진경제", "촉진제", "진통제", "거담제", "항혈소판제",
                    "소염효소제", "항생제", "고혈압약", "H2 차단제", "해열제", "소염제", "위산과다증약",
                    "구충제", "칼슘제", "진경제", "혈압강하제", "진해거담제", "비염약", "소화성궤양용제",
                    "기타의소화기관용약", "항악성종양제", "최면진정제", "동맥경화용제",
                    "정신신경용제", "기타의중추신경용약", "기타의비뇨생식기관및항문용약", "당뇨병용제", "치과구강용약",
                    "기타의순환계용약", "간장질환용제", "혼합비타민제차",
                    "기타의비타민제", "기타의화학요법제", "무기질제제", "기타의자양강장변질제", "하제|완장제",
                    "골격근이완제", "따로분류되지않는대사성의약품", "항전간제", "제산제", "주로그람양성|음성균에작용하는것",
                    "주로그람양성균|리케치아|비루스에작용하는것", "비타민C및P제", "단백아미노산제제", "항히스타민제", "종합대사성제제",
                    "혈관확장제", "기타의외피용약", "정장제", "비타민E및K제", "기타의알레르기용약",
                    "자격요법제(비특이성면역억제제를포함)", "난포호르몬제및황체호르몬제", "효소제제", "기타의호흡기관용약", "안과용제",
                    "합성마약", "통풍치료제", "따로분류되지않고치료를주목적으로하지않는의약품", "기타의혈액및체액용약", "부정맥용제",
                    "비타민B제(비타민B1을제외)", "비타민B1제", "건위소화제", "자율신경제", "아편알카로이드계제제", "혈액응고저지제",
                    "진훈제", "피임제", "항원충제", "기타의조제용약", "최토제|진토제", "뇌하수체호르몬제", "각성제|흥분제", "해독제",
                    "비타민A및D제", "기타의조직세포의기능용의약품", "기타의항생물질제제(복합항생물질제제를포함)", "부신호르몬제", "이비과용제",
                    "강심제", "화농성질환용제", "모발용제(발모|탈모|염모|양모제)", "주로그람양성|음성균|리케치아|비루스에작용하는것", "지혈제",
                    "항결핵제", "주로항산성균에작용하는것", "이담제", "비뇨생식기관용제(성병예방제포함)", "주로그람음성균에작용하는것", "설화제",
                    "혈관보강제", "단백동화스테로이드제", "이뇨제", "치질용제", "주로그람양성균에작용하는것", "치나제", "기타의종양치료제",
                    "기타의호르몬제(항호르몬제를포함)", "혈액대용제", "자궁수축제", "갑상선|부갑상선호르몬제", "장기제제", "기생성피부질환용제",
                    "기타의신경계및감각기관용의약품", "주로곰팡이|원충에작용하는것", "혈관수축제", "X선조영제", "면역혈청학적검사용시약", "후란계제제",
                    "기타의말초신경용약", "호흡기관용약", "백신류", "소화기관용약",
            };*/

            String pharmacyName = null;     // 약국명
            String dispensingYear = null;   // 조제일자(년)
            String dispensingMonth = null;  // 조제일자(월)
            String dispensingDay = null;    // 조제일자(일)
            Pattern datePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");    // xxxx-xx-xx로 표기된 날짜
            List<String> detectedCategories = new ArrayList<>();   // 약제 종류
            Pattern categoryPattern = Pattern.compile("[\\(\\[]([ㄱ-ㅎ|가-힣|a-z|A-Z|0-9]*[제약])[\\)\\]]"); // (), [] 안에 있으면서 ~제 또는 ~약으로 끝나는 text
            boolean voiceQR = false;        // 음성복약지도 큐알 여부
            int[] threeTimes = new int[3];

            for (Text.TextBlock block : text.getTextBlocks()) {
                String blockText = block.getText();

                // 약국
                if (pharmacyName == null && blockText.contains("약국")) {
                    String[] words = blockText.split(" ");
                    for (String word: words) {
                        if (word.endsWith("약국") && ! word.equals("약국")) {   // 'xx약국' 단어 찾기
                            pharmacyName =  word;
                            break;
                        }
                    }
                }

                blockText = blockText.replaceAll("\\s", "");   // 공백 전부 제거

                // 조제일자
                if (dispensingYear == null && blockText.contains("조제일자")) {
                    Matcher dispensingDateMatcher = datePattern.matcher(blockText);
                    if (dispensingDateMatcher.find()) {
                        dispensingYear = dispensingDateMatcher.group(1);
                        dispensingMonth = dispensingDateMatcher.group(2);
                        dispensingDay = dispensingDateMatcher.group(3);
                    }
                }

                // 음성 복약 지도
                if (!voiceQR && blockText.contains("음성복약지도")) {
                    voiceQR = true;
                }

                // 약제 종류
                Matcher categoryMatcher = categoryPattern.matcher(blockText);
                while (categoryMatcher.find()) {
                    detectedCategories.add(categoryMatcher.group(1));
                }

                // 아침 점심 저녁
                if (blockText.equals("아침")) {
                    threeTimes[0]++;
                } else if (blockText.equals("점심")) {
                    threeTimes[1]++;
                } else if (blockText.equals("저녁")) {
                    threeTimes[2]++;
                }
            }
            
            if (dispensingYear == null && detectedCategories.size() == 0 && ! voiceQR) {    // 약포지
                speakDetectedPharmacyName(pharmacyName);
                speakDetectedThreeTimes(threeTimes);
            } else {    // 약국 봉투
                speakDetectedVoiceQR(voiceQR);
                speakDetectedPharmacyNameAndDispensingDate(pharmacyName, dispensingYear, dispensingMonth, dispensingDay);
                speakDetectedCategories(detectedCategories);
            }
        }

        private void speakDetectedPharmacyName(String pharmacyName) {
            if (pharmacyName != null){
                tts.speak(String.format("%s에서 만들어진 약 봉투입니다.", pharmacyName), QUEUE_FLUSH, null, null);
            }
        }

        private void speakDetectedThreeTimes(int[] threeTimes) {
            StringBuilder ThreeTimesGuide = new StringBuilder();
            ThreeTimesGuide.append("사진에서 ");
            if (threeTimes[0] != 0) {
                ThreeTimesGuide.append("아침 ").append(threeTimes[0]).append("개, ");
            }
            if (threeTimes[1] != 0) {
                ThreeTimesGuide.append("점심 ").append(threeTimes[1]).append("개, ");
            }
            if (threeTimes[2] != 0) {
                ThreeTimesGuide.append("저녁 ").append(threeTimes[2]).append("개, ");
            }
            ThreeTimesGuide.append("의 단어가 발견되었습니다.");
            tts.speak(ThreeTimesGuide.toString(), QUEUE_ADD, null, null);
        }

        private void speakDetectedVoiceQR(boolean voiceQR) {
            if (voiceQR) {
                tts.speak("음성복약지도 큐알코드가 발견되었습니다. 보이스아이 어플을 통해 세부 복약 정보를 음성으로 확인하실 수 있습니다.", QUEUE_ADD, null, null);
            }
        }

        private void speakDetectedPharmacyNameAndDispensingDate(String pharmacyName, String dispensingYear, String dispensingMonth, String dispensingDay) {
            if (pharmacyName != null && dispensingYear != null) {
                tts.speak(String.format("%s년 %s월 %s일, %s에서 만들어진 약 봉투입니다.", dispensingYear, dispensingMonth, dispensingDay, pharmacyName), QUEUE_FLUSH, null, null);
            } else if (pharmacyName != null){
                tts.speak(String.format("%s에서 만들어진 약 봉투입니다.", pharmacyName), QUEUE_FLUSH, null, null);
            } else if (dispensingYear != null) {
                tts.speak(String.format("%s년 %s월 %s일에 제조된 약 봉투입니다.", dispensingYear, dispensingMonth, dispensingDay), QUEUE_FLUSH, null, null);
            } else {
                tts.speak("약국과 제조일자에 대한 정보를 찾을 수 없습니다.", QUEUE_FLUSH, null, null);
            }
        }

        private void speakDetectedCategories(List<String> detectedCategories) {
            if (detectedCategories.size() > 0) {
                StringBuilder sb = new StringBuilder();

                sb.append("조회된 약제 종류는 ");
                for (String category: detectedCategories) {
                    sb.append(category).append(", ");
                }
                sb.append("입니다.");
                tts.speak(sb, QUEUE_ADD, null, PHARMACY_ENVELOPE_LAST_GUIDE);
            } else {
                tts.speak("약제 종류 정보를 찾을 수 없습니다.", QUEUE_ADD, null, PHARMACY_ENVELOPE_LAST_GUIDE);
            }
        }
    }
}
