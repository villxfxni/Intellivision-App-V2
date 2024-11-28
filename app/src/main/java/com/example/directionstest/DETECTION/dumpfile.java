package com.example.directionstest.DETECTION;

public class dumpfile {
    /*
    package com.example.directionstest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    private Interpreter tflite;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1001;
    private TextToSpeech textToSpeech;
    private boolean isTtsEnabled = true;
    private ObjectDetector objectDetector;
    private PreviewView cameraView;
    private SurfaceView overlayView;
    private Rect focusBox;
    private List<String> labels;
    private long lastProcessedTime = 0;
    private static final long PROCESS_INTERVAL_MS = 1500;
    private final Map<String, Long> lastSpokenTimes = new HashMap<>();
    private static final long TTS_COOLDOWN_MS = 1800;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeTextToSpeech();


        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();

        objectDetector = ObjectDetection.getClient(options);
        overlayView = findViewById(R.id.overlay_view);
        if (overlayView != null) {
            overlayView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    Log.d("SurfaceView", "Surface created");
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                    Log.d("SurfaceView", "Surface changed");
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    Log.d("SurfaceView", "Surface destroyed");
                }
            });
        }
        Log.d("TAGG", "ObjectDetectorOptions initialized");
        objectDetector = ObjectDetection.getClient(options);
        focusBox = new Rect(300, 800, 800, 1200);


        cameraView = findViewById(R.id.camera_view);
        if (cameraView == null) {
            Log.e("CameraSetup", "PreviewView is null. Check your XML layout.");
            return;
        }
        Button startCam = findViewById(R.id.btnCam);
        startCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 1001);
                } else {
                    initializeTFLite();
                }
            }
        });
        findViewById(R.id.tts_toggle_button).setOnClickListener(v -> {
            isTtsEnabled = !isTtsEnabled;
            String status = isTtsEnabled ? "TTS ACTIVADO" : "TTS DESACTIVADO";
            speak(status);
        });
    }

    private void loadLabels(String filename) {
        labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(filename)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            Log.e("TFLite", "Error reading label file", e);
        }
    }

    private void initializeTFLite() {
        try {
            tflite = new Interpreter(loadModelFile("detect.tflite"));
            loadLabels("labelmap.txt");
            Log.d("TFLite", "Model and labels loaded successfully");
            setupCamera();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing TensorFlow Lite", e);
        }
    }

    private MappedByteBuffer loadModelFile(String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void setupCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                // Configure the camera selector
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Configure the ImageAnalysis use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480)) // Adjust resolution if needed
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    try {
                        processImage(imageProxy);
                    } catch (Exception e) {
                        Log.e("ImageAnalysis", "Error processing image", e);
                    } finally {
                        imageProxy.close(); // Always close the imageProxy
                    }
                });

                // Bind the use cases to the lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap resizeBitmapForModel(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 300, 300, true);
    }
    public void processImage(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessedTime < PROCESS_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        lastProcessedTime = currentTime;
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        Bitmap resizedBitmap = resizeBitmapForModel(bitmap);

        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(resizedBitmap);
        float[][][] outputLocations = new float[1][10][4];
        float[][] outputClasses = new float[1][10];
        float[][] outputScores = new float[1][10];
        float[] numDetections = new float[1];

        Object[] inputs = {tensorImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputLocations);
        outputs.put(1, outputClasses);
        outputs.put(2, outputScores);
        outputs.put(3, numDetections);

        tflite.runForMultipleInputsOutputs(inputs, outputs);
        drawBoundingBoxes(outputLocations, outputClasses, outputScores[0], numDetections[0]);
        imageProxy.close();
    }
    private void drawBoundingBoxes(float[][][] locations, float[][] classes, float[] scores, float numDetections) {

        if (overlayView == null || overlayView.getHolder() == null) {
            Log.e("DrawBoundingBoxes", "SurfaceView or SurfaceHolder is null");
            return;
        }

        int width = overlayView.getWidth();
        int height = overlayView.getHeight();
        Log.d("DrawBoundingBoxes", "OverlayView dimensions: width=" + width + ", height=" + height);

        Canvas canvas = overlayView.getHolder().lockCanvas();
        overlayView.setZOrderOnTop(true);
        overlayView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        if (canvas == null) {
            Log.e("DrawBoundingBoxes", "Canvas is null");
            return;
        }

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        if(canvas != null){
        for (int i = 0; i < numDetections; i++) {
            if (scores[i] > 0.55) {
                float[] box = locations[0][i];
                float left = box[1] * width;
                float top = box[0] * height;
                float right = box[3] * width;
                float bottom = box[2] * height;

                Log.d("DrawBoundingBoxes", String.format(
                        "Detection %d: Confidence=%.2f, Rect=[Left=%.2f, Top=%.2f, Right=%.2f, Bottom=%.2f]",
                        i, scores[i], left, top, right, bottom));

                canvas.drawRect(left, top, right, bottom, boxPaint);
                String label = labels.get((int) classes[0][i]);
                float centerX = (left + right) / 2;

                // Determine position
                String position;
                if (centerX < width / 3) {
                    position = "a la izquierda";
                } else if (centerX > 2 * width / 3) {
                    position = "a la derecha";
                } else {
                    position = " en frente";
                }

                String ttsMessage = label + " " + position;
                speak(ttsMessage);
                Log.d("DrawBoundingBoxes", "Label: " + label);
                canvas.drawText(label + " (" + scores[i] + ")", left, top - 10, textPaint);
            } else {
                //Log.d("DrawBoundingBoxes", "Detection " + i + " skipped due to low confidence: " + scores[i]);
            }
        }
        overlayView.getHolder().unlockCanvasAndPost(canvas);
        Log.d("DrawBoundingBoxes", "Canvas updated with bounding boxes.");
    }
}

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
          Bitmap outputBitmap = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888);
        YuvToRgbConverter.convert(imageProxy, outputBitmap);

        return outputBitmap;
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("es", "MX"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }

    private void speak(String text) {
        if (isTtsEnabled && textToSpeech != null) {
            long currentTime = System.currentTimeMillis();
            if (lastSpokenTimes.containsKey(text) && (currentTime - lastSpokenTimes.get(text)) < TTS_COOLDOWN_MS) {
                return;
            }
            lastSpokenTimes.put(text, currentTime);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
        if (tflite != null) tflite.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}


     *
     *
     *
    * */
}
