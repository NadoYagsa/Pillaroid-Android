package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ImageReader;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;

import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.nadoyagsa.pillaroid.env.ImageUtils;
import com.nadoyagsa.pillaroid.tflite.Classifier;
import com.nadoyagsa.pillaroid.tflite.DetectorFactory;
import com.nadoyagsa.pillaroid.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SearchCaseActivity extends ObjectDetectionCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(300, 300);    // tflite에서 설정한 값대로 변경
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private Classifier detector;
    private MultiBoxTracker tracker;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Matrix frameToCropTransform;

    private boolean computingDetection = false;
    private long timestamp = 0;
    private long currTimestamp;

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
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

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        currTimestamp = timestamp;

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

        runInBackground(detectRunnable);
    }

    Runnable detectRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("Object-Detection", "Running detection on image " + currTimestamp);
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);    // 사물인식 좌표: 0~1 사이 값

            final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();

            for (final Classifier.Recognition result : results) {
                RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF) {
                    boolean rotated = getScreenOrientation() % 180 == 90;
                    int rotatedFrameWidth = rotated ? previewHeight : previewWidth;
                    int rotatedFrameHeight = rotated ? previewWidth : previewHeight;

                    // frame 크기로 사물인식 좌표 변경
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

            computingDetection = false;

            // 가이드를 위한 분석 시작
            if (!isWaitingForGuide) {
                isWaitingForGuide = true;
                guide();
            }
        }
    };

    private void guide() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_QR_CODE)   // 13자리 숫자 형태, QR 형태
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        MlImage mlImage = new BitmapMlImageBuilder(rgbFrameBitmap).build();
        scanner.process(mlImage)
                .addOnSuccessListener(barcodes -> {
                    String code = null;
                    for (Barcode barcode : barcodes) {
                        int valueType = barcode.getValueType();
                        if (valueType == Barcode.TYPE_PRODUCT) {
                            code = barcode.getDisplayValue();
                            Log.e("resultBarcodeCode", code);

                            tts.speak("바코드가 인식되었습니다.", QUEUE_FLUSH, null, API_SUCCESS);
                            tts.playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, null);

                            Intent medicineIntent = new Intent(this, MedicineResultActivity.class);
                            medicineIntent.putExtra("barcode", code);
                            startActivity(medicineIntent);
                        }
                    }
                    if (code == null) {
                        // 인식된 바코드가 없을 경우
                        tts.speak("바코드가 인식되지 않았습니다. 천천히 상하좌우로 움직여주세요.", QUEUE_FLUSH, null, IS_GUIDING);
                    }
                });
    }

    @Override
    protected int getLayoutId() { return R.layout.fragment_camera_connection; }

    @Override
    protected Size getDesiredPreviewFrameSize() { return DESIRED_PREVIEW_SIZE; }
}
