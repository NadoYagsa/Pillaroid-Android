package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SearchPrescriptionActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_prescription);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                checkCameraPermission();    // 카메라로 인식을 위한 카메라 퍼미션 체크
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        /* TODO: 처방전 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, PrescriptionResultActivity.class));
    }

    private void checkCameraPermission() {
        // 카메라 권한이 없으면 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_DENIED) {
            tts.speak("처방전인식을 위해 카메라 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
            tts.speak("권한 거부 시에는 메인 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1000);
        }
        else {
            tts.speak("후면 카메라가 켜졌습니다. 처방전을 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
            startCamera();  //카메라 실행
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1000 && grantResults.length>0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tts.speak("후면 카메라가 켜졌습니다. 처방전을 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
                startCamera();
            }
            else
                finish();
        }
    }

    private void startCamera() {
        PreviewView pvPrescriptionCamera = findViewById(R.id.pv_search_prescription);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                // Set up the capture use case to allow users to take photos
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(((LifecycleOwner) this), cameraSelector, preview, imageCapture);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(pvPrescriptionCamera.getSurfaceProvider());
            } catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
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
