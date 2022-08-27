package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import static com.nadoyagsa.pillaroid.SearchCameraActivity.RESULT_PERMISSION_DENIED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SearchCaseActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    private TextToSpeech tts;

    private PreviewView pvCaseCamera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Executor caseExecutor;
    private CaseAnalyzer caseAnalyzer;
    private ImageProxy currentImageProxy = null;

    private Boolean canUseCamera = null;
    private boolean isAnalyzing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_case);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                // 모든 퍼미션이 있을 때에만 카메라가 켜짐
                if(hasPermission()){
                    canUseCamera = true;
                    tts.speak("후면 카메라와 플래시가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
                    startCamera();  // 카메라 실행
                } else{ // 모든 권한이 허가되지 않았다면 요청
                    tts.speak("의약품 용기를 찍기 위해선 카메라 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                    tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak("권한 거부 시에는 이전 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

                    ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA}, REQUEST_CODE_PERMISSIONS);
                }
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        pvCaseCamera = findViewById(R.id.pv_search_case);
        caseAnalyzer = new CaseAnalyzer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        tts.speak("후면 카메라와 플래시가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
    }

    private void startCamera() {
        caseExecutor = Executors.newSingleThreadExecutor(); // 카메라 시작시 executor도 실행

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // camera preview로 보여줄 view finder 설정
        Preview preview = new Preview.Builder().build();

        // 카메라 선택 (렌즈 앞/뒤)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 사진 캡쳐 관련 설정
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // 화질을 기준으로 최적화
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())   // rotation은 디바이스의 기본 설정에 따름
                .build();

        // 위에서 만든 설정 객체들로 useCaseGroup 만들기
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build();

        // useGroup으로 카메라 객체 생성
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        camera.getCameraControl().enableTorch(true);    // flash

        preview.setSurfaceProvider(pvCaseCamera.getSurfaceProvider());  // 영상(preview)을 PreviewView에 연결
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (isAnalyzing) {
                tts.speak("아직 이전 사진을 분석 중입니다. 조금 뒤에 시도해주세요.", QUEUE_FLUSH, null, null);
            } else {
                // 사진 찍기
                imageCapture.takePicture(caseExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        isAnalyzing = true;
                        currentImageProxy = imageProxy;
                        tts.speak("사진이 찍혔습니다. 이미지를 분석합니다.", QUEUE_FLUSH, null, null);

                        // 이미지 분석
                        caseAnalyzer.analyze(imageProxy);

                        isAnalyzing = false;
                    }
                });
            }
            return true;    // 볼륨 UP 기능 없앰
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;    // 볼륨 DOWN 기능 없앰
        }
        return super.onKeyDown(keyCode, event);
    }

    private class CaseAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            scanBarcode(imageProxy);
        }
    }

    private void scanBarcode(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            // InputImage 객체에 분석할 이미지 받기
            InputImage inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();
            BarcodeScanner scanner = BarcodeScanning.getClient(options);
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        String code = null;
                        for (Barcode barcode: barcodes) {
                            int valueType = barcode.getValueType();
                            if (valueType == Barcode.TYPE_PRODUCT) {
                                code = barcode.getDisplayValue();
                                Log.d("resultBarcodeCode", code);

                                tts.speak("바코드가 인식되었습니다.", QUEUE_FLUSH, null, null);
                                tts.playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, null);

                                imageProxy.close(); // 바코드 인식됐으므로 imageProxy 종료
                                currentImageProxy = null;

                                Intent medicineIntent = new Intent(this, MedicineResultActivity.class);
                                medicineIntent.putExtra("barcode", code);
                                startActivity(medicineIntent);
                            }
                        }
                        if (code == null) {
                            // 인식된 바코드가 없을 경우
                            tts.speak("바코드가 인식되지 않았습니다. 다시 촬영을 진행합니다.", QUEUE_FLUSH, null, null);
                        }
                        imageProxy.close(); // 바코드 인식되면 imageProxy 닫음
                        currentImageProxy = null;
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(grantResults)) {
                canUseCamera = true;
                tts.speak("후면 카메라와 플래시가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
                startCamera();  // 권한 모두 허가되면 startCamera 실행
            } else {
                Log.e("Camera", "Permissions not granted by the user");
                setResult(RESULT_PERMISSION_DENIED);
                this.finish();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void closeCamera() {
        if (cameraProviderFuture != null && caseExecutor != null){
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
            caseExecutor = null;
            if (currentImageProxy != null) {
                currentImageProxy.close();
                currentImageProxy = null;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (canUseCamera != null && canUseCamera) {
            startCamera();  // 카메라 실행
        } else if (canUseCamera != null && !canUseCamera){
            tts.speak("의약품 용기를 찍기 위해선 카메라 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
            tts.speak("권한 거부 시에는 이전 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();  // 카메라 자원 해제
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // tts 자원 해제
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
            closeCamera();  // 카메라 자원 해제
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
