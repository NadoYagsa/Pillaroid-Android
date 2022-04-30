package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SearchCaseActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };

    private TextToSpeech tts;

    private PreviewView pvCaseCamera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor caseExecutor;
    private CaseAnalyzer caseAnalyzer;
    private TextRecognizer recognizer;

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

                tts.speak("후면 카메라가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        pvCaseCamera = findViewById(R.id.pv_search_case);
        caseExecutor = Executors.newSingleThreadExecutor();
        caseAnalyzer = new CaseAnalyzer();
        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        if(allPermissionsGranted()){
            startCamera();  //카메라 실행
        } else{ //모든 권한이 허가되지 않았다면 요청
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        /* TODO: 용기를 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, MedicineResultActivity.class));
    }

    private void startCamera() {
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

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(Surface.ROTATION_270)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   //ImageProxy.close()시 그때의 최신 이미지를 전달함
                .build();
        imageAnalysis.setAnalyzer(caseExecutor, caseAnalyzer);

        final ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        preview.setSurfaceProvider(pvCaseCamera.getSurfaceProvider());

        cameraProvider.unbindAll();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

        //TODO: 볼륨 버튼 리스너 (imageCapture.takePicture로 캡처)
    }

    private class CaseAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy imageProxy) {
            detectText(imageProxy);
            //scanBarcode(imageProxy);
        }
    }

    private void detectText(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            Task<Text> result = recognizer.process(inputImage)
                    .addOnSuccessListener(text -> {
                        Log.d("ML TEXT - success", "!!");
                        //Task completed successfully
                        String resultText = text.getText();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            String blockText = block.getText(); //block 별 인식되는 텍스트
                            Point[] blockCornerPoints = block.getCornerPoints();    //block 별 인식되는 영역 경계 좌표
                            Rect blockFrame = block.getBoundingBox();
                            Log.d("ML Text - block", blockText);
                            for (Text.Line line : block.getLines()) {
                                String lineText = line.getText();   //line 별 인식되는 텍스트
                                Point[] lineCornerPoints = line.getCornerPoints();  //line 별 인식되는 영역 경계 좌표
                                Rect lineFrame = line.getBoundingBox();
                                Log.d("ML Text - line", lineText);
                                for (Text.Element element : line.getElements()) {
                                    String elementText = element.getText();     //element 별 인식되는 텍스트
                                    Point[] elementCornerPoints = element.getCornerPoints();    //element 별 인식되는 영역 경계 좌표
                                    Rect elementFrame = element.getBoundingBox();
                                    Log.d("ML Text - element", elementText);
                                }
                            }
                        }
                        imageProxy.close(); //CameraX 사용시 반드시 해줘야 함
                    })
                    .addOnFailureListener(e -> {
                        //Task failed with an exception
                    });
        }
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();  //권한 모두 허가되면 startCamera 실행
            } else {
                Log.e("Camera", "Permissions not granted by the user");
                tts.speak("카메라 권한이 승인되지 않아 기능을 사용할 수 없습니다.", QUEUE_FLUSH, null, null);
                this.finish();
            }
        }
    }

    private void closeCamera() {
        if (cameraProviderFuture != null && caseExecutor != null){
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
            //caseExecutor.shutdown();
            caseExecutor = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();  //카메라 실행
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();  //카메라 자원 해제
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
            closeCamera();  //카메라 자원 해제
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
