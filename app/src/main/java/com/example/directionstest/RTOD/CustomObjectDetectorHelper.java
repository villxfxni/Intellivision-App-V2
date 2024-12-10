package com.example.directionstest.RTOD;

import android.util.Size;
import android.view.View;

import com.example.directionstest.UI.Views.BoundingBox;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.List;

public class CustomObjectDetectorHelper {
    private BoundingBox box;
    private LocalModel localModel;
    private ObjectDetector objectDetector;

    public CustomObjectDetectorHelper(int detectorMode, float confidenceThreshold, int maxLabelPerObject, String modelName){
        loadModel(modelName);
        initializeDetector(detectorMode, confidenceThreshold, maxLabelPerObject);
    }

    private void loadModel(String modelName){
        localModel = new LocalModel.Builder()
                .setAssetFilePath(modelName+".tflite")
                .build();
    }

    private void initializeDetector(int detectorMode, float confidenceThreshold, int maxLabelPerObject) {
        CustomObjectDetectorOptions customObjectDetectorOptions = new CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(detectorMode) // CustomObjectDetectorOptions.STREAM_MODE
                .enableClassification()
                .setClassificationConfidenceThreshold(confidenceThreshold)
                .setMaxPerObjectLabelCount(maxLabelPerObject)
                .build();

        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
    }


    public void closeDetector(){
        box.setVisibility(View.GONE);
        objectDetector.close();
    }

    public void setView(BoundingBox box){
        this.box = box;
        box.setVisibility(View.VISIBLE);
    }

    public BoundingBox getView(){
        return box;
    }
    public Task<List<DetectedObject>> process(InputImage image) {
        return objectDetector.process(image);
    }
}
