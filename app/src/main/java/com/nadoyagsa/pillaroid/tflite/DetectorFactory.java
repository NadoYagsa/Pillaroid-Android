package com.nadoyagsa.pillaroid.tflite;

import android.content.res.AssetManager;

import java.io.IOException;

public class DetectorFactory {
    public static YoloV5Classifier getDetector(
            final AssetManager assetManager,
            final String modelFilename)
            throws IOException {
        String labelFilename = null;
        boolean isQuantized = false;
        int inputSize = 0;

        if (modelFilename.equals("pill-fp16.tflite")) {
            labelFilename = "file:///android_asset/pill_label.txt";
            isQuantized = false;
            inputSize = 640;
        }
        // TODO: 바코드 관련 tflite 생기면 else if문으로 작성

        return YoloV5Classifier.create(assetManager, modelFilename, labelFilename, isQuantized, inputSize);
    }

}
