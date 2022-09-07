package com.nadoyagsa.pillaroid.tflite;

import static com.nadoyagsa.pillaroid.ObjectDetectionCameraActivity.MINIMUM_CONFIDENCE_TF;
import static com.nadoyagsa.pillaroid.env.Utils.loadModelFile;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TFLiteClassifier implements Classifier {
    private static final int NUM_DETECTIONS = 10;
    private boolean isModelQuantized;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    private static final int NUM_THREADS = 4;

    private int inputSize;
    private Vector<String> labels = new Vector<>();
    private int[] intValues;

    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS, 4]
    // contains the location of detected boxes
    private TensorBuffer outputLocations;
    private int[] outputLocationsShape;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private TensorBuffer outputClasses;
    private int[] outputClassesShape;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private TensorBuffer outputScores;
    private int[] outputScoresShape;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private TensorBuffer numDetections;
    private int[] numDetectionsShape;

    private ByteBuffer imgData;
    private Interpreter tfLite;

    public static Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final boolean isQuantized,
            final int inputSize)
            throws IOException {
        final TFLiteClassifier d = new TFLiteClassifier();

        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        Interpreter.Options options = (new Interpreter.Options());
        options.setNumThreads(NUM_THREADS);

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.inputSize = inputSize;
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.outputLocationsShape = new int[]{1, NUM_DETECTIONS, 4};
        d.outputClassesShape = new int[]{1, NUM_DETECTIONS};
        d.outputScoresShape = new int[]{1, NUM_DETECTIONS};
        d.numDetectionsShape = new int[]{1};
        return d;
    }

    private TFLiteClassifier() {}

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {    // bitmap: croppedBitmap
        convertBitmapToByteBuffer(bitmap);  // imgData(ByteBuffer)에 저장

        outputLocations = TensorBuffer.createFixedSize(outputLocationsShape, DataType.FLOAT32);
        outputClasses = TensorBuffer.createFixedSize(outputClassesShape, DataType.FLOAT32);
        outputScores = TensorBuffer.createFixedSize(outputScoresShape, DataType.FLOAT32);
        numDetections = TensorBuffer.createFixedSize(numDetectionsShape, DataType.FLOAT32);

        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations.getBuffer());
        outputMap.put(1, outputClasses.getBuffer());
        outputMap.put(2, outputScores.getBuffer());
        outputMap.put(3, numDetections.getBuffer());

        Object[] inputArray = {imgData};
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        return processOutputs(outputScores, outputClasses, outputLocations);
    }

    private List<Recognition> processOutputs(TensorBuffer scores, TensorBuffer classes, TensorBuffer locations) {
        float[] scoresFloatArray = scores.getFloatArray();
        float[] classesFloatArray = classes.getFloatArray();
        float[] locationsFloatArray = locations.getFloatArray();
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < locationsFloatArray.length; i += 4) {
            if (scoresFloatArray[ i / 4 ] >= getObjThresh()) {
                final RectF detection =
                        new RectF(
                                locationsFloatArray[i + 1],
                                locationsFloatArray[i],
                                locationsFloatArray[i + 3],
                                locationsFloatArray[i + 2]
                        );
                recognitions.add(
                        new Recognition(
                                "" + i / 4,
                                labels.get((int) classesFloatArray[ i / 4 ]),
                                scoresFloatArray[ i / 4 ],
                                detection));
            }
        }
        return recognitions;
    }

    protected ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();   // position 0
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else {
                    // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        return imgData;
    }

    @Override
    public int getInputSize() {
        return inputSize;
    }

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {
        tfLite.close();
        tfLite = null;
    }

    @Override
    public float getObjThresh() {
        return MINIMUM_CONFIDENCE_TF;
    }

}
