package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SearchPrescriptionActivity extends AppCompatActivity {
    private boolean canUseCamera = false;
    private TextToSpeech tts;

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

                pvPrescriptionCamera = findViewById(R.id.pv_search_prescription);
                recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
                prescriptionAnalyzer = new PrescriptionAnalyzer();

                checkCameraPermission();    // 카메라로 인식을 위한 카메라 퍼미션 체크
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        /* TODO: 처방전 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, PrescriptionResultActivity.class));
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
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)     // 사진 캡처를 지연 시간을 기준으로 최적화 (cf. 화질 기준: CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)             // 후면 카메라 사용
                        .build();
                
                ViewPort viewPort = pvPrescriptionCamera.getViewPort();                 // 실제 화면의 촬영 부분과 동일하게 사진을 인식함

                cameraProvider.unbindAll();

                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(preview)
                        //.addUseCase(imageAnalysis)
                        .addUseCase(imageCapture)
                        .setViewPort(viewPort)
                        .build();

                Camera camera = cameraProvider.bindToLifecycle(((LifecycleOwner) this), cameraSelector, useCaseGroup);

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
                            Log.i("텍스트 OCR - success", "!!");

                            String resultText = text.getText();
                            Log.i("텍스트 OCR - text", resultText);
                            for (Text.TextBlock block : text.getTextBlocks()) {         // block 별
                                String blockText = block.getText();
                                Point[] blockCornerPoints = block.getCornerPoints();
                                Rect blockFrame = block.getBoundingBox();
                                Log.i("텍스트 OCR - block", blockText);

                                for (Text.Line line : block.getLines()) {               // line 별
                                    String lineText = line.getText();
                                    Point[] lineCornerPoints = line.getCornerPoints();
                                    Rect lineFrame = line.getBoundingBox();
                                    Log.i("텍스트 OCR - line", lineText);

                                    for (Text.Element element : line.getElements()) {   // element 별
                                        String elementText = element.getText();
                                        Point[] elementCornerPoints = element.getCornerPoints();
                                        Rect elementFrame = element.getBoundingBox();
                                        // Log.i("텍스트 OCR - element", elementText);
                                    }
                                }
                            }

                            imageProxy.close();
                        })
                        .addOnFailureListener(Throwable::printStackTrace);
            }
        }
    }

    // 위 볼륨 버튼으로 사진 촬영
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 촬영 버튼 클릭
                imageCapture.takePicture(ContextCompat.getMainExecutor(SearchPrescriptionActivity.this), new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        // 처방전 사진에서 텍스트 인식 요망
                        prescriptionAnalyzer.analyze(image);
                        super.onCaptureSuccess(image);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        super.onError(exception);
                    }
                });

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
