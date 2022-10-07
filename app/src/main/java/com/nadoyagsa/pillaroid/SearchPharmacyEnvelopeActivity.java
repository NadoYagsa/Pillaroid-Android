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

import java.util.concurrent.ExecutionException;

public class SearchPharmacyEnvelopeActivity  extends AppCompatActivity {

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
                    .addOnSuccessListener(new PharmacyEnvelopeSuccessListner())
                    .addOnFailureListener(e -> {
                        tts.speak("약국 봉투의 텍스트를 읽는데 오류가 발생했습니다.", QUEUE_FLUSH, null, null);
                        isSearching = false;
                    });
        }
    }

    private class PharmacyEnvelopeSuccessListner implements OnSuccessListener<Text> {
        @Override
        public void onSuccess(Text text) {
            isSearching = false;
        }
    }
}
