package com.nadoyagsa.pillaroid.tflite;

import android.content.res.AssetManager;

import java.io.IOException;

public class DetectorFactory {
    public static Classifier getDetector(
            final AssetManager assetManager,
            final String modelFilename)
            throws IOException {
        String labelFilename = null;
        boolean isQuantized = false;
        int inputSize = 0;

        if (modelFilename.equals("pill-fp16.tflite")) {     // yoloV5에서 만든 tflite로, out을 한 객체에 담아 내보냄 (xPos, yPos, confidence)
            labelFilename = "file:///android_asset/pill_label.txt";
            isQuantized = false;
            inputSize = 640;

            return YoloV5Classifier.create(assetManager, modelFilename, labelFilename, isQuantized, inputSize);
        }  else if (modelFilename.equals("hand.tflite")) {   // out을 네 객체에 담아 내보냄 (locations, scores, classes, numDetections)
            labelFilename = "file:///android_asset/hand_label.txt";
            isQuantized = false;
            inputSize = 300;

            return TFLiteClassifier.create(assetManager, modelFilename, labelFilename, isQuantized, inputSize);
        } else {
            throw new IllegalArgumentException();
        }
    }

}
