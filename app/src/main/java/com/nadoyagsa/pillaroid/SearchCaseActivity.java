package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private Executor caseExecutor;
    private CaseAnalyzer caseAnalyzer;
    private ImageProxy currentImageProxy = null;

    private boolean isReadyCamera = false;
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

                if (isReadyCamera) {
                    tts.speak("후면 카메라와 플래시가 켜졌습니다. 손에 의약품을 잡고 카메라 뒤로 위치시켜주세요.", QUEUE_FLUSH, null, null);
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
        isReadyCamera = true;
        caseExecutor = Executors.newSingleThreadExecutor(); //카메라 시작시 executor도 실행

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

        //사진 캡쳐 관련 설정
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) //화질을 기준으로 최적화
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())   //rotation은 디바이스의 기본 설정에 따름
                .build();

        //위에서 만든 설정 객체들로 useCaseGroup 만들기
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build();

        //useGroup으로 카메라 객체 생성
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        camera.getCameraControl().enableTorch(true);    //flash

        preview.setSurfaceProvider(pvCaseCamera.getSurfaceProvider());  //영상(preview)을 PreviewView에 연결
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (isAnalyzing) {
                tts.speak("아직 이전 사진을 분석 중입니다. 조금 뒤에 시도해주세요.", QUEUE_FLUSH, null, null);
            } else {
                //사진 찍기
                imageCapture.takePicture(caseExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        isAnalyzing = true;
                        currentImageProxy = imageProxy;
                        tts.speak("사진이 찍혔습니다. 이미지를 분석합니다.", QUEUE_FLUSH, null, null);

                        //이미지 분석
                        caseAnalyzer.analyze(imageProxy);

                        isAnalyzing = false;
                    }
                });
            }
            return true;    //볼륨 UP 기능 없앰
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;    //볼륨 DOWN 기능 없앰
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
            //InputImage 객체에 분석할 이미지 받기
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
                                tts.playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, null);   // 0.4초 딜레이

                                imageProxy.close(); //바코드 인식됐으므로 imageProxy 종료
                                currentImageProxy = null;

                                Intent medicineIntent = new Intent(this, MedicineResultActivity.class);
                                medicineIntent.putExtra("barcode", code);
                                startActivity(medicineIntent);
                            }
                        }
                        if (code == null) {
                            detectText(imageProxy); //바코드 인식된 게 없다면 제품명 인식
                        } else {
                            imageProxy.close(); //바코드 인식되면 텍스트 인식하지 않고 imageProxy 닫음
                            currentImageProxy = null;
                        }
                    });
        }
    }

    private void detectText(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            //InputImage 객체에 분석할 이미지 받기
            InputImage inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            TextRecognizer recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            recognizer.process(inputImage)
                    .addOnSuccessListener(text -> {
                        //Task completed successfully
                        int maxHeight = 0;
                        Text.TextBlock findMaxHeightBlock = null;

                        String resultText = text.getText(); //TODO: 삭제
                        Log.d("resultText", resultText);    //TODO: 삭제
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            Rect blockFrame = block.getBoundingBox();

                            assert blockFrame != null;
                            int height = blockFrame.height();
                            if (height > maxHeight) {
                                maxHeight = height;
                                findMaxHeightBlock = block;
                            }
                        }

                        if (findMaxHeightBlock != null) {
                            tts.speak("인식된 의약품 이름은 " + findMaxHeightBlock.getText() + "입니다.", QUEUE_FLUSH, null, null);
                            //TODO: 추가 작업 필요 (height만으로 판단하면 X(정방향이 아니게 찍을 경우, 세로형 텍스트가 있을 경우..), 폰트 특이한건 인식 X)
                            //TODO: 추출한 텍스트가 제품명이 아님을 알 수 있어야 함
                        }
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close(); //text 인식까지 모두 끝나면 imageProxy 닫기
                        currentImageProxy = null;
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
        isReadyCamera = false;
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
