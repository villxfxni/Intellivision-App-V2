package com.example.directionstest.RTOD;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.directionstest.UI.Views.BoundingBox;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.ArrayList;
import java.util.List;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.directionstest.UI.Views.BoundingBox;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.ArrayList;
import java.util.List;
public class FrameAnalyzer implements ImageAnalysis.Analyzer {
    private Image mediaImage;
    private final CustomObjectDetectorHelper customDetectorHelper;
    private ObjectDetectionListener objectDetectionListener;
    private List<String> previousDetectedObjects = new ArrayList<>();

    public FrameAnalyzer(int detectionMode, int detectorMode) {
        customDetectorHelper = new CustomObjectDetectorHelper(CustomObjectDetectorOptions.STREAM_MODE,
                0.5f, 2, "object_labeler");
    }

    public void closeDetector() {
        customDetectorHelper.closeDetector();
    }

    public void setView(BoundingBox box) {
        customDetectorHelper.setView(box);
    }

    public void setPreviewResolution(Size res) {
        customDetectorHelper.getView().setPreviewResolution(res);
    }

    public void setInputResolution(Size res) {
        customDetectorHelper.getView().setInputResolution(res);
    }

    @Override
    @ExperimentalGetImage
    public void analyze(ImageProxy imageProxy) {
        mediaImage = imageProxy.getImage();
        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        Task<List<DetectedObject>> task = customDetectorHelper.process(image);

        task.addOnSuccessListener(detectedObjects -> {
            if (!detectedObjects.isEmpty()) {
                List<DetectedObjectData> detectedDataList = new ArrayList<>();
                for (DetectedObject obj : detectedObjects) {
                    if (!obj.getLabels().isEmpty()) {
                        String objectName = obj.getLabels().get(0).getText();
//                        String objectName2=obj.getLabels().get(1).getText();
//                        Log.d("TAG",objectName2);
                        String position = calculatePosition(obj);
                        detectedDataList.add(new DetectedObjectData(objectName, position));
                    }
                }
                if (objectDetectionListener != null) {
                    objectDetectionListener.onObjectsDetected(detectedDataList);
                }
            }
            customDetectorHelper.getView().setDetectedObjects(detectedObjects);
        }).addOnFailureListener(e -> {
            customDetectorHelper.getView().postInvalidate();
            Log.e("TAG", "analyze: unable to detect");
        }).addOnCompleteListener(result -> imageProxy.close());
    }
    private String calculatePosition(DetectedObject obj) {
        // Get normalized centerX (0 = leftmost, 1 = rightmost)
        float centerX = (float) (obj.getBoundingBox().left + obj.getBoundingBox().right) / 2;
        float previewWidth = customDetectorHelper.getView().getPreviewResolution().getWidth();
        float normalizedCenterX = centerX / previewWidth;
        Log.d("TAG", "CENTER "+centerX+" LEFT"+obj.getBoundingBox().left+ " RIGHT"+ obj.getBoundingBox().right + " PREVIEW W "+ previewWidth);
        if (normalizedCenterX < (previewWidth/2)-300) {
            return "a la izquierda";
        } else if (normalizedCenterX > (previewWidth/2)+300) {
            return "a la derecha";
        } else {
            return "en frente";
        }
    }



    public interface ObjectDetectionListener {
        void onObjectsDetected(List<DetectedObjectData> detectedObjects);
    }

    public void setObjectDetectionListener(ObjectDetectionListener listener) {
        this.objectDetectionListener = listener;
    }

    public static class DetectedObjectData {
        public final String objectName;
        public final String position;

        public DetectedObjectData(String objectName, String position) {
            this.objectName = objectName;
            this.position = position;
        }
    }
}