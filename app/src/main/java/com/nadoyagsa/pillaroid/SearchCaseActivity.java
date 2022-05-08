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
import android.view.KeyEvent;

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
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
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

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        //camera preview로 보여줄 view finder 설정
        Preview preview = new Preview.Builder().build();

        //카메라 선택 (렌즈 앞/뒤)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //촬영시 이미지 분석 관련 설정(TextRecognizer, ScanBarcode)
        imageAnalysis = new ImageAnalysis.Builder().build();

        //사진 캡쳐 관련 설정
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) //화질을 기준으로 최적화
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())   //rotation은 디바이스의 기본 설정에 따름
                .build();

        //위에서 만든 설정 객체들로 카메라 객체 생성
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture); //이미지 분석, 이미미 캡쳐

        preview.setSurfaceProvider(pvCaseCamera.getSurfaceProvider());  //preview를 PreviewView에 연결
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            //사진 찍기
            imageCapture.takePicture(caseExecutor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    //사진 분석 초기화
                    imageAnalysis.clearAnalyzer();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(SearchCaseActivity.this), caseAnalyzer);
                }
            });
            return true;    //볼륨 UP 기능 없앰
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;    //볼륨 DOWN 기능 없앰
        }
        return super.onKeyDown(keyCode, event);
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
            //InputImage 객체에 분석할 이미지 받기
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
//                        imageProxy.close(); //이미지 캡쳐시에는 필요 X (연속적으로 분석하는 경우 필요)
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
            caseExecutor = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //모든 퍼미션이 있을 때에만 카메라가 켜짐
        if(allPermissionsGranted()){
            startCamera();  //카메라 실행
        } else{ //모든 권한이 허가되지 않았다면 요청
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
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
