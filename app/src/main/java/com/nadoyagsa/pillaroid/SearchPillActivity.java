package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import com.nadoyagsa.pillaroid.tflite.YoloV5Classifier;
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
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);    // tflite에서 설정한 값대로 변경
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;

    private YoloV5Classifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        final float textSizepx =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizepx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final String modelString = "pill-fp16.tflite";  // 사물인식 돌릴 tflite 파일

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

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

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
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas1 = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.0f);

            final List<Classifier.Recognition> mappedRecognitions =
                    new LinkedList<>();

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                    canvas1.drawRect(location, paint);

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);
                }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            if (mappedRecognitions.size() != 0) {
                Collections.sort(mappedRecognitions);   // confidence 기준으로 정렬
                for (Classifier.Recognition recognition : mappedRecognitions) {
                    if (recognition.getTitle().equals("pill")) {    // pill로 인식한 것 중 가장 신뢰율이 높은 결과에 대해 음성 가이드
                        RectF location = recognition.getLocation();
                        tracker.getFrameToCanvasMatrix().mapRect(location);

                        boolean rotation = !(getScreenOrientation() % 180 == 0);
                        int frameWidth = rotation ? previewHeight : previewWidth;
                        int frameHeight = rotation ? previewWidth : previewHeight;

                        Log.e("debug", String.format("box location (%f, %f) -> (%f, %f)", location.left, location.top, location.right, location.bottom));
                        Log.e("debug", String.format("cameraLayout width x height: %d x %d", frameWidth, frameHeight));
                        Log.e("debug", String.format("object width x height: %d x %d", (int) (location.right - location.left), (int) (location.bottom - location.top)));

                        float objectWidth = location.right - location.left;
                        float objectHeight = location.bottom - location.top;

                        int boundaryWidth = (int) (frameWidth * 0.05);
                        int boundaryHeight = (int) (frameHeight * 0.05);

                        boolean isNormal = true;

                        // 위치 가이드
                        if (location.top < boundaryHeight) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too over");
                            tts.speak("알약이 너무 위에 있습니다. 손바닥을 조금만 아래로 내려주세요.", QUEUE_ADD, null, null);
                        } else if (location.bottom > frameHeight - boundaryHeight) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too under");
                            tts.speak("알약이 너무 아래에 있습니다. 손바닥을 조금만 위로 올려주세요.", QUEUE_ADD, null, null);
                        } else if (location.left < boundaryWidth) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too left");
                            tts.speak("알약이 너무 왼쪽에 있습니다. 손바닥을 조금만 오른쪽으로 이동해 주세요.", QUEUE_ADD, null, null);
                        } else if (location.right > frameWidth - boundaryWidth) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too right");
                            tts.speak("알약이 너무 오른쪽에 있습니다. 손바닥을 조금만 왼쪽으로 이동해 주세요.", QUEUE_ADD, null, null);
                        }

                        // 거리 가이드
                        if (objectWidth < 30 || objectHeight < 30) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too far");
                            tts.speak("알약이 너무 멀리 있습니다. 손바닥을 조금만 가까이 대주세요.", QUEUE_ADD, null, null);
                        } else if (objectWidth > 300 || objectHeight > 300) {
                            isNormal = false;
                            Log.e("Object-Detection-result", "too close");
                            tts.speak("알약이 너무 가까이에 있습니다. 손바닥을 조금만 멀리 대주세요.", QUEUE_ADD, null, null);
                        }

                        if (isNormal == true) {
                            Log.i("Object-Detection-result", "normal");
                            tts.speak("알약이 잘 인식되었습니다. 인식 결과를 가져오는 중입니다. 조금만 기다려주세요.", QUEUE_ADD, null, null);

                            // TODO: 이미지 서버에게 보내기
                        }

                        break;
                    }
                }
            } else {
                Log.e("Object-Detection-result", "no detection");
                tts.speak("알약이 인식되지 않습니다. 알약을 포착할 수 있도록 카메라를 더 멀리 이동해주세요.", QUEUE_ADD, null, null);
            }
        };
        runInBackground(runnable);
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
