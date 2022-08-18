package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;
import static com.nadoyagsa.pillaroid.SearchCameraActivity.RESULT_PERMISSION_DENIED;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.nadoyagsa.pillaroid.env.ImageUtils;

import java.nio.ByteBuffer;
import java.util.Locale;

public abstract class ObjectDetectionCameraActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener, Camera.PreviewCallback, View.OnClickListener {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    protected TextToSpeech tts;

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    protected Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pill_camera);

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
                    setFragment();
                    tts.speak("후면 카메라와 플래시가 켜졌습니다. 손바닥 위에 알약을 올려놓고 카메라를 들어주세요. 현재 영상을 기준으로 가이드를 안내할 예정입니다.", QUEUE_FLUSH, null, null);
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

//        if (hasPermission()) {
//            setFragment();
//        } else {
//            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_CAMERA}, REQUEST_CODE_PERMISSIONS);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(grantResults)) {
                  setFragment();
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

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    /* Callback for android.hardware.Camera API */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            Log.w("Object-Detection", "Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                () -> ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

        postInferenceCallback =
                () -> {
                    camera.addCallbackBuffer(bytes);
                    isProcessingFrame = false;
                };
        processImage();
    }

    /* Callback for Camera2 API */
    @Override
    public void onImageAvailable(ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    () -> ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

            postInferenceCallback =
                    () -> {
                        image.close();
                        isProcessingFrame = false;
                    };

            processImage();
        } catch (final Exception e) {
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) { }

        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL); // TODO: 영은 camera2 되는지 확인 (소은: false여서 legacy만 확인됨)
                Log.i("Object-Detection", String.format("Camera API lv2?: %s", useCamera2API));
                return cameraId;
            }
        } catch (CameraAccessException e) {
            Log.e("Object-Detection", "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            Log.e("pillaroid-debug", "camera2");
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            (size, rotation) -> {   // Camera Connection Callback
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                ObjectDetectionCameraActivity.this.onPreviewSizeChosen(size, rotation);
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            Log.e("pillaroid-debug", "legacy");
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.fl_search_pill, fragment).commit();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                Log.d("Object-Detection", String.format("Initializing buffer %d at size %d", i, buffer.capacity()));
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            // 레퍼런스에서 ROTATION 270<->180, 90<->0 변경
            case Surface.ROTATION_270:
                return 180;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_90:
                return 0;
            default:
                return 90;
        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) { }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();
}
