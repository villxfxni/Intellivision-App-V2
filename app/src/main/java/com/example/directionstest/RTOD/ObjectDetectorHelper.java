package com.example.directionstest.RTOD;

import com.example.directionstest.UI.Views.BoundingBox;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

public class ObjectDetectorHelper {
    private BoundingBox box;
    private ObjectDetector objectDetector;
    public static final int CLASSIFY_SINGLE_OBJECT = 0;
    public static final int CLASSIFY_MULTIPLE_OBJECTS = 1;

    public void initializeObjectDetector(int detectionMode){
        //TODO: HANDLE CLOSE
        ObjectDetectorOptions options;
        if(detectionMode == CLASSIFY_SINGLE_OBJECT) {
            options = new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .build();
        }
        else {
            options = new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .enableMultipleObjects()
                    .build();
        }

        objectDetector = ObjectDetection.getClient(options);
    }

}
