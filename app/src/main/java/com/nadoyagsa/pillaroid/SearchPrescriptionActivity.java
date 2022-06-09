package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

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

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SearchPrescriptionActivity extends AppCompatActivity {
    private boolean canUseCamera = false;
    private boolean isSearching = false;

    private TextToSpeech tts;
    private TextView tvGuide;

    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView pvPrescriptionCamera;
    private PrescriptionAnalyzer prescriptionAnalyzer;
    private TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_prescription);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
                prescriptionAnalyzer = new PrescriptionAnalyzer();

                checkCameraPermission();    // 카메라로 인식을 위한 카메라 퍼미션 체크
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        tvGuide = findViewById(R.id.tv_search_prescription_guide);
        pvPrescriptionCamera = findViewById(R.id.pv_search_prescription);
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= 21) {
            // 카메라 권한이 없으면 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                tts.speak("처방전 인식을 위해 카메라 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("권한 거부 시에는 메인 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1000);
            } else {
                canUseCamera = true;

                tts.speak("후면 카메라가 켜졌습니다. 처방전을 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
                startCamera();  //카메라 실행
            }
        } else {
            tts.speak("SDK 버전이 낮아 카메라 사용이 불가합니다.", TextToSpeech.QUEUE_ADD, null, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                canUseCamera = true;

                tts.speak("후면 카메라가 켜졌습니다. 처방전을 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
                startCamera();
            } else
                finish();
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // CameraProvider 사용 가능 여부 확인
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                /*  // 볼륨 버튼을 사용하지 않는다면 아래의 코드가 필요함:)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetRotation(Surface.ROTATION_270)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(SearchPrescriptionActivity.this), prescriptionAnalyzer);
                */

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)     // 사진 캡처를 지연 시간을 기준으로 최적화 (cf. 화질 기준: CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)             // 후면 카메라 사용
                        .build();

                cameraProvider.unbindAll();

                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(preview)
                        //.addUseCase(imageAnalysis)
                        .addUseCase(imageCapture)
                        .build();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);

                preview.setSurfaceProvider(pvPrescriptionCamera.getSurfaceProvider());
            } catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    private class PrescriptionAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError")
            Image prescriptionImage = imageProxy.getImage();

            if (prescriptionImage != null) {
                InputImage image = InputImage.fromMediaImage(prescriptionImage, imageProxy.getImageInfo().getRotationDegrees());

                Task<Text> result = recognizer.process(image)
                        .addOnSuccessListener(text -> {
                            Log.i("텍스트 OCR - success", "성공적");

                            String resultText = text.getText();
                            ArrayList<PrescriptionPosition> itemList = new ArrayList<>();

                            int medicineNameLeftX = -1;
                            int medicineNameBottomY = -1;
                            int medicineFinishBottomY = -1;

                            for (Text.TextBlock block : text.getTextBlocks()) {         // block 별
                                String blockText = block.getText();
                                Point[] blockCornerPoints = block.getCornerPoints();        // 왼위, 오위, 오아래, 왼아래

                                assert blockCornerPoints != null;
                                if (blockText.replaceAll("\\s", "").contains("의약품의명칭")) {
                                    medicineNameLeftX = blockCornerPoints[0].x;
                                    medicineNameBottomY = blockCornerPoints[2].y;
                                }
                                else {
                                    // 의약품의 명칭보다 아래 위치하며 왼쪽에 적힌 문장들
                                    if (medicineNameBottomY != -1 && blockCornerPoints[0].x<=medicineNameLeftX && blockCornerPoints[0].y>=medicineNameBottomY) {
                                        // blockText에서 실제 의약품 명칭만 빼냄
                                        String medicine = blockText
                                                .replaceAll("[^0-9a-zA-Z가-힣/()\\[\\]]", "")     // 숫자, 영소/대문자, 한글, /[]() 를 제외한 문자는 모두 제거
                                                .replace('[', '(')
                                                .replace(']', ')')
                                                .replace("(약)", "")                             // (약) 문자 제거
                                                .replaceAll("\\(?[비급여]{1,3}\\)", "")           // 급여 관련 키워드 제거  ex. 비), (비급여), (급여)
                                                .replaceAll("\\(?[0-9]{9}\\)?", "")              // 보험 코드(ex. 661604420)
                                                .replaceAll("^\\([0-9]*\\)", "")                 // 순번 제거
                                                .replaceAll("\\([^)]*\\)?", "")                   // 모든 괄호 내용 제거
                                                .trim();

                                        //TODO: 의약품 대신 보험 코드가 있을 때, 보험 코드를 쓰는 것이 나을지 고민해보자!
                                        itemList.add(new PrescriptionPosition(blockCornerPoints[0].y, medicine));
                                    }
                                    // 의약품의 명칭보다 아래 위치하며 오른쪽에 적힌 문장들
                                    else if (medicineNameBottomY != -1 && blockCornerPoints[0].x>medicineNameLeftX && blockCornerPoints[0].y>=medicineNameBottomY) {
                                        if (blockText.contains("주사제 처방내역") || blockText.contains("처방내역") || blockText.contains("원내조제")) {            // 주사제 처방내역 위의 부분까지만 의약품 명칭임
                                            medicineFinishBottomY = blockCornerPoints[2].y;
                                            break;
                                        }
                                        else if (blockText.contains("이하여백") || blockText.contains("이하 여백") ) {
                                            medicineFinishBottomY = blockCornerPoints[2].y;
                                            break;
                                        }
                                    }
                                }
                            }

                            // 의약품 명칭만 추출함
                            ArrayList<String> medicineList = new ArrayList<>();
                            for (PrescriptionPosition item : itemList) {
                                if (medicineFinishBottomY > item.topY)
                                    medicineList.add(item.medicineName);
                            }

                            if (medicineList.size() > 0) {
                                /*
                                tts.speak("조회할 처방 의약품은 ", TextToSpeech.QUEUE_FLUSH, null, null);
                                for (String medicine: medicineList) {
                                    tts.speak(medicine, TextToSpeech.QUEUE_ADD, null, null);
                                    tts.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, null);
                                }
                                tts.speak("로, 총 "+medicineList.size()+"개 입니다.", TextToSpeech.QUEUE_ADD, null, null);
                                tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);
                                 */

                                imageProxy.close();

                                /* 처방전 인식 후 검색 결과 확인 */
                                Intent resultIntent = new Intent(SearchPrescriptionActivity.this, PrescriptionResultActivity.class);
                                resultIntent.putStringArrayListExtra("medicineList", medicineList);
                                tts.stop();
                                startActivity(resultIntent);
                            }
                            else
                                tts.speak("인식된 처방 의약품이 없습니다.", TextToSpeech.QUEUE_FLUSH, null, null);

                            imageProxy.close();

                            isSearching = false;
                        })
                        .addOnFailureListener(e -> {
                            Log.i("Failure", "사진 속 텍스트 인식 오류");
                            tts.speak("처방전의 텍스트를 읽는데 오류가 발생했습니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                            tvGuide.setText("텍스트 인식 오류가 발생했습니다.");
                        });
            }
        }
    }

    // 위 볼륨 버튼으로 사진 촬영
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 촬영 버튼 클릭
                if (!isSearching) {
                    isSearching = true;
                    tts.speak("사진이 찍혔습니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                    tvGuide.setText("처방전 사진 속 약품명 인식 중");

                    imageCapture.takePicture(ContextCompat.getMainExecutor(SearchPrescriptionActivity.this), new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            super.onCaptureSuccess(image);

                            // 처방전 사진에서 텍스트 인식
                            prescriptionAnalyzer.analyze(image);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            isSearching = false;
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

        if (canUseCamera)
            startCamera();      // 다시 카메라 실행
        else
            checkCameraPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 카메라 자원 해제
        if (cameraProviderFuture != null) {
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
        }
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

            // 카메라 자원 해제
            if (cameraProviderFuture != null) {
                cameraProviderFuture.cancel(true);
                cameraProviderFuture = null;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class PrescriptionPosition {
        private final int topY;
        private final String medicineName;

        public PrescriptionPosition(int topY, String medicineName) {
            this.topY = topY;
            this.medicineName = medicineName;
        }
    }
}
