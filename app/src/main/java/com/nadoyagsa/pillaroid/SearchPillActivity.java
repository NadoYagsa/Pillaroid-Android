package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import com.nadoyagsa.pillaroid.customview.OverlayView;
import com.nadoyagsa.pillaroid.env.BorderedText;
import com.nadoyagsa.pillaroid.env.ImageUtils;
import com.nadoyagsa.pillaroid.tflite.Classifier;
import com.nadoyagsa.pillaroid.tflite.DetectorFactory;
import com.nadoyagsa.pillaroid.tracking.MultiBoxTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchPillActivity extends ObjectDetectionCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(300, 300);    // tflite에서 설정한 값대로 변경
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
//    OverlayView trackingOverlay;  // 화면에 인식된 객체 나타내기

    private Classifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;

    private MultiBoxTracker tracker;

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        final float textSizepx =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizepx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final String modelString = "hand.tflite";  // 사물인식 돌릴 tflite 파일

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (IOException e) {   // Classifier could not be initialized
            e.printStackTrace();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        int sensorOrientation = getScreenOrientation();
        Log.i("Object-Detection", String.format("Camera orientation relative to screen canvas: %d", sensorOrientation));

        Log.i("Object-Detection", String.format("Initializing at size %dx%d", previewWidth, previewHeight));
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);


//        trackingOverlay = findViewById(R.id.tracking_overlay);
//        trackingOverlay.addCallback(
//                canvas -> {
//                    tracker.draw(canvas);
//                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
//        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        Log.i("Object-Detection", "Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        Runnable runnable = () -> {
            Log.i("Object-Detection", "Running detection on image " + currTimestamp);
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);    // 사물인식 좌표: 0~1 사이 값

            final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();

            for (final Classifier.Recognition result : results) {
                RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF) {
                    boolean rotated = getScreenOrientation() % 180 == 90;
                    int rotatedFrameWidth = rotated ? previewHeight : previewWidth;
                    int rotatedFrameHeight = rotated ? previewWidth : previewHeight;

                    location = new RectF(
                            Math.max(result.getLocation().left * rotatedFrameWidth, 1),
                            Math.max(result.getLocation().top * rotatedFrameHeight, 1),
                            Math.min(result.getLocation().right * rotatedFrameWidth, rotatedFrameWidth),
                            Math.min(result.getLocation().bottom * rotatedFrameHeight, rotatedFrameHeight));

                    result.setLocation(location);
                    mappedRecognitions.add(result);
                }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
//            trackingOverlay.postInvalidate();

            computingDetection = false;

            // 가이드를 위한 분석 시작
            if (!isWaitingForGuide) {
                if (mappedRecognitions.size() == 0) {
                    Log.e("Object-Detection-result", "no detection");
                    tts.speak("손이 감지되지 않습니다. 손을 포착할 수 있도록 카메라를 더 멀리 이동해주세요.", QUEUE_FLUSH, null, "is-guiding");
                } else {
                    Collections.sort(mappedRecognitions);   // confidence 기준으로 정렬

                    // confidence가 가장 높은 hand Recognition 찾기
                    Classifier.Recognition recognition = mappedRecognitions.stream()
                            .filter(r -> r.getTitle().equals("hand"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("detected object 중 hand가 없습니다."));


                    RectF location = recognition.getLocation();
                    float objectWidth = location.right - location.left;
                    float objectHeight = location.bottom - location.top;

                    boolean rotated = getScreenOrientation() % 180 == 90;
                    int rotatedFrameWidth = rotated ? previewHeight : previewWidth;
                    int rotatedFrameHeight = rotated ? previewWidth : previewHeight;

                    int boundaryWidth = (int) (rotatedFrameWidth * 0.2);
                    int boundaryHeight = (int) (rotatedFrameHeight * 0.3);

                    Log.e("pillaroid-debug", String.format("box location (%f, %f) -> (%f, %f)", location.left, location.top, location.right, location.bottom));
                    Log.e("pillaroid-debug", String.format("cameraLayout width x height: %d x %d", rotatedFrameWidth, rotatedFrameHeight));
                    Log.e("pillaroid-debug", String.format("object width x height: %d x %d", (int) objectWidth, (int) objectHeight));

                    // 거리 가이드
                    boolean isNormal = true;
                    if ((objectWidth < rotatedFrameWidth / 2) || (objectHeight < rotatedFrameHeight / 2)) {
                        isNormal = false;
                        Log.e("Object-Detection-result", "too far");
                        tts.speak("손이 너무 멀리 있습니다. 손바닥을 조금만 가까이 대주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    }

                    // 위치 가이드 (여백 정도에 따른 가이드)
                    if (location.bottom < rotatedFrameHeight - boundaryHeight) {   // TODO: 알약이 보통 손바닥 위에 있음을 감안하여 boundary를 줄일지
                        isNormal = false;
                        Log.e("Object-Detection-result", "too over");
                        tts.speak("손이 너무 위에 있습니다. 손바닥을 조금만 아래로 내려주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    } else if (location.top > boundaryHeight) {
                        isNormal = false;
                        Log.e("Object-Detection-result", "too under");
                        tts.speak("손이 너무 아래에 있습니다. 손바닥을 조금만 위로 올려주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    } else if (location.right < rotatedFrameWidth - boundaryWidth) {
                        isNormal = false;
                        Log.e("Object-Detection-result", "too left");
                        tts.speak("손이 너무 왼쪽에 있습니다. 손바닥을 조금만 오른쪽으로 이동해 주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    } else if (location.left > boundaryWidth) {
                        isNormal = false;
                        Log.e("Object-Detection-result", "too right");
                        tts.speak("손이 너무 오른쪽에 있습니다. 손바닥을 조금만 왼쪽으로 이동해 주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    }

                    if (isNormal) {
                        Log.i("Object-Detection-result", "normal");
                        tts.speak("알약이 잘 감지되었습니다. 인식 결과를 가져오는 중입니다.", QUEUE_FLUSH, null, API_SUCCESS);

                        // 서버에게 이미지 보내기
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmSS");
                        String time = dateFormat.format(Calendar.getInstance().getTime());

                        byte[] resultBytes = bitmapToByteArray(rgbFrameBitmap);
                        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), resultBytes);
                        MultipartBody.Part pillImage = MultipartBody.Part.createFormData("pillImage", time + ".jpg", requestBody);

                        isWaitingForGuide = true;
                        PillaroidAPIImplementation.getApiService().postPillByImage(pillImage).enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.code() == 200) {
                                    int idx = 0;
                                    try {
                                        JSONObject medicineJson = new JSONObject(Objects.requireNonNull(response.body()));
                                        Log.i("detected-pill-serialNumber", medicineJson.toString());

                                        idx = Integer.parseInt(medicineJson.getJSONObject("data")
                                                .getString("medicineIdx"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Intent medicineIntent = new Intent(SearchPillActivity.this, MedicineResultActivity.class);
                                    medicineIntent.putExtra("medicineIdx", idx);
                                    startActivity(medicineIntent);
                                } else if (response.code() == 400 || response.code() == 404) {    // 400: flask로 이미지가 전달되지 않음, 404: yolov5에 의해 crop된 알약이 없음
                                    isWaitingForGuide = false;
                                    Log.e("api-response", "bad image");
                                    tts.speak("알약 인식에 실패했습니다. 다시 시도해주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                                } else if (response.code() == 500) {
                                    Log.e("api-response", "service error");
                                    tts.speak("서비스 오류로 인해 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                                } else {
                                    isWaitingForGuide = false;
                                    Log.e("api-response", "case: " + response.code());
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                Log.e("api-response", "no service");
                                tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, API_FAILED);
                            }
                        });
                    }
                }
            }
        };
        runInBackground(runnable);
    }

    // Bitmap을 Byte로 변환
    public byte[] bitmapToByteArray( Bitmap bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        return stream.toByteArray();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_camera_connection;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }
}