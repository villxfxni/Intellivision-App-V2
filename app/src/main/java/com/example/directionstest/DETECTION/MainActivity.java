package com.example.directionstest.DETECTION;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.ImageButton;

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

import com.example.directionstest.Menu;
import com.example.directionstest.R;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;

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
    private TextToSpeech tts;
    private boolean ttsActivado = true;
    private ObjectDetector objectDetector;
    private PreviewView cameraView;
    private SurfaceView overlayView;
    private Rect focusBox;
    private List<String> labels;
    private long intervalo = 0;
    private static final long PROCESS_INTERVAL_MS = 850;
    private final Map<String, Long> ultimoSpeak = new HashMap<>();
    private static final long TTS_COOLDOWN_MS = 1000;
    private Button btnTTS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarTTS();

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
                    Log.d("SurfaceView", "Surface creado");
                }
                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                    Log.d("SurfaceView", "Surface cambiado");
                }
                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    Log.d("SurfaceView", "Surface destroyed");
                }
            });
        }
        objectDetector = ObjectDetection.getClient(options);
        focusBox = new Rect(300, 800, 800, 1200);

        cameraView = findViewById(R.id.camera_view);
        if (cameraView == null) {
            Log.e("TAGG", "CAMARA NO DISPONIBLE.");
            return;
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 1001);
        } else {
            iniciarTFLite();

        }
       btnTTS= findViewById(R.id.btnTTS);
        btnTTS.setOnClickListener(v -> {
            ttsActivado = !ttsActivado;
            String status = ttsActivado ? "TTS ACTIVADO" : "TTS DESACTIVADO";
            btnTTS.setText(ttsActivado ? "DESACTIVAR": "REACTIVAR");
            speak(status);
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1)
        {
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                iniciarTFLite();
            }
        }
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

    private void iniciarTFLite() {
        try {
            tflite = new Interpreter(loadModelFile("detect.tflite"));
            loadLabels("labelmap.txt");
            Log.d("TFLite", "Modelos cargados");
            setupCamera();
        } catch (IOException e) {
            throw new RuntimeException("Error iniciando TensorFlow Lite", e);
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
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    try {
                        processImage(imageProxy);
                    } catch (Exception e) {
                        Log.e("ImageAnalysis", "Error procesando imagen", e);
                    } finally {
                        imageProxy.close();
                    }
                });
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Error iniciando camara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap reescalarBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 300, 300, true);
    }
    public void processImage(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - intervalo < PROCESS_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        intervalo = currentTime;
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        Bitmap resizedBitmap = reescalarBitmap(bitmap);

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
            Log.e("TAGG", "Surfaceview nulo");
            return;
        }

        int width = overlayView.getWidth();
        int height = overlayView.getHeight();

        Canvas canvas = overlayView.getHolder().lockCanvas();
        overlayView.setZOrderOnTop(true);
        overlayView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        if (canvas == null) {
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
                if (scores[i] > 0.538) {
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
                    canvas.drawText(label + " (" + scores[i] + ")", left, top - 10, textPaint);
                } else {
                    //Log.d("DrawBoundingBoxes", "Detection " + i + " skipped due to low confidence: " + scores[i]);
                }
            }
            overlayView.getHolder().unlockCanvasAndPost(canvas);
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

    private void inicializarTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("es", "MX"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }

    private void speak(String text) {
        if (ttsActivado && tts != null) {
            long currentTime = System.currentTimeMillis();
            if (ultimoSpeak.containsKey(text) && (currentTime - ultimoSpeak.get(text)) < TTS_COOLDOWN_MS) {
                return;
            }
            ultimoSpeak.put(text, currentTime);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
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

