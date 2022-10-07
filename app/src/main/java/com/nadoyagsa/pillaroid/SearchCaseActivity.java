package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Surface;

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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SearchCaseActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String BARCODE_SUCCESS = "barcode-success";
    private static final String BARCODE_FAILED = "barcode-failed";

    private PreviewView pvCaseCamera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor caseExecutor;
    private CaseAnalyzer caseAnalyzer;
    private ImageProxy currentImageProxy = null;

    private String code = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_case);

        pvCaseCamera = findViewById(R.id.pv_search_case);
        caseAnalyzer = new CaseAnalyzer();
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

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(Surface.ROTATION_270)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   // ImageProxy.close() 시 그 때의 최신 이미지 전달
                .build();
        imageAnalysis.setAnalyzer(caseExecutor, caseAnalyzer);

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .addUseCase(imageCapture)
                .build();

        preview.setSurfaceProvider(pvCaseCamera.getSurfaceProvider());  // 영상(preview)을 PreviewView에 보이도록 설정

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
    }

    private class CaseAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError")
            Image mediaImage = imageProxy.getImage();

            if (mediaImage == null) {
                return;
            }

            currentImageProxy = imageProxy;

            InputImage inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());  // InputImage 객체에 분석할 이미지 받기

            scanBarcode(inputImage);
        }
    }

    private void scanBarcode(InputImage inputImage) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    boolean isDetected = false;
                    for (Barcode barcode: barcodes) {
                        int valueType = barcode.getValueType();
                        if (valueType == Barcode.TYPE_PRODUCT) {
                            isDetected = true;

                            code = barcode.getDisplayValue();
                            Log.d("resultBarcodeCode", code);

                            tts.speak("바코드가 인식되었습니다.", QUEUE_FLUSH, null, BARCODE_SUCCESS); // tts utteranceProgressListener에서 후처리
                        }
                    }
                    if (! isDetected) {
                        if (! tts.isSpeaking()) {
                            tts.speak("바코드가 인식되지 않았습니다. 용기를 천천히 움직여주세요.", QUEUE_FLUSH, null, BARCODE_FAILED);   // tts utteranceProgressListener에서 후처리
                        }
                        currentImageProxy.close();
                    }
                })
                .addOnFailureListener(barcode -> {
                    currentImageProxy.close();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(grantResults)) {
                tts.speak("후면 카메라가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
                startCamera();
            } else {
                Log.e("Camera", "Permissions not granted by the user");
                tts.speak("카메라 권한이 승인되지 않아 기능을 사용할 수 없습니다.", QUEUE_FLUSH, null, null);
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
        if (cameraProviderFuture != null){
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
        }
        if (caseExecutor != null) {
            caseExecutor = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals(BARCODE_SUCCESS)) {
                    Intent medicineIntent = new Intent(SearchCaseActivity.this, MedicineResultActivity.class);
                    medicineIntent.putExtra("barcode", code);
                    startActivity(medicineIntent);
                } else if (utteranceId.equals(BARCODE_FAILED)) {
                    currentImageProxy.close();
                }
            }

            @Override
            public void onError(String utteranceId) { }
        });

        if (hasPermission()) {
            tts.speak("후면 카메라가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
            startCamera();
        } else {
            tts.speak("의약품 용기를 찍기 위해선 카메라 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", QUEUE_ADD, null, null);
            tts.speak("권한 거부 시에는 이전 화면으로 돌아갑니다.", QUEUE_ADD, null, null);

            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }
}