package com.example.directionstest.DETECTION;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Size;
import android.widget.Button;
import android.widget.ImageButton;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.directionstest.R;
import com.example.directionstest.RTOD.FrameAnalyzer;
import com.example.directionstest.RTOD.ObjectDetectorHelper;
import com.example.directionstest.UI.Views.BoundingBox;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private FrameAnalyzer analyzer;
    private BoundingBox boundingBox;
    private Camera camera;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;

    private Button btnTTS;
    private ImageButton btnBack;

    // Text-to-Speech
    private TextToSpeech textToSpeech;
    private boolean ttsActivado = true;

    private final Queue<String> ttsQueue = new LinkedList<>();
    private boolean isSpeaking = false;

    private final Map<String, String> translationMap = new HashMap<String, String>() {{
        put("cat", "gato");
        put("dog", "perro");
        put("bottle", "botella");
        put("car", "coche");
        put("electronic device", "objeto");
        put("person", "persona");
        put("table", "mesa");
        put("chair", "silla");
        put("laptop", "laptop");
        put("cup", "taza");
        put("mouse", "mouse");
        put(" ", "objeto");
        put("furniture", "mueble");
        put("computer keyboard", "teclado");
        put("shelf", "estante");
        put("cabinetry", "gabinete");
        put("curtain", "cortina");
        put("stuffed toy", "peluche");
        put("toy", "juguete");
        put("headphones", "audífonos");
        put("camera", "cámara");
        put("packaged goods", "producto");
        put("knife", "cuchillo");
        put("houseplant", "planta de interior");
        put("potted plant", "planta en maceta");
        put("flower", "flor");
        put("plant pot", "maceta");
        put("book", "libro");
        put("bicycle", "bicicleta");
        put("bag", "bolsa");
        put("umbrella", "paraguas");
        put("watch", "reloj");
        put("shoe", "zapato");
        put("hat", "sombrero");
        put("clothing", "ropa");
        put("sofa", "sofá");
        put("remote control", "control remoto");
        put("television", "televisor");
        put("monitor", "monitor");
        put("phone", "teléfono");
        put("tablet", "tableta");
        put("pen", "pluma");
        put("pencil", "lápiz");
        put("backpack", "mochila");
        put("ball", "pelota");
        put("mirror", "espejo");
        put("plate", "plato");
        put("spoon", "cuchara");
        put("fork", "tenedor");
        put("bowl", "tazón");
        put("bed", "cama");
        put("door", "puerta");
        put("window", "ventana");
        put("lamp", "lámpara");
        put("cushion", "cojín");
        put("blanket", "manta");
        put("box", "caja");
        put("trash can", "basurero");
        put("basket", "canasta");
        put("fan", "ventilador");
        put("clock", "reloj");
        put("footwear","zapato");
        put("christmas tree","arbol de navidad");
        put("container", "contenedor");
    }};

    @Override
    @ExperimentalGetImage
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        previewView = findViewById(R.id.previewView);
        boundingBox = findViewById(R.id.boundingBox);
        btnTTS = findViewById(R.id.btnTTS);
        btnBack = findViewById(R.id.btnBack);
        inicializarTTS();
        permCheck();
        // Botón para activar/desactivar TTS
        btnTTS.setOnClickListener(v -> {
            ttsActivado = !ttsActivado;
            String status = ttsActivado ? "TTS ACTIVADO" : "TTS DESACTIVADO";
            btnTTS.setText(ttsActivado ? "DESACTIVAR" : "REACTIVAR");
            speak(status);
        });

        // Botón para cerrar el Activity
        btnBack.setOnClickListener(v -> finish());
    }

    private void permCheck() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.CAMERA
            }, 1001);
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) setupCameraPreview();
        } else {
            setupCameraPreview();
        }
    }


    private void setupCameraPreview() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {

                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                buildCameraPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void buildCameraPreview() {
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(360, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);

        setupRealtimeDetection();
    }
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void setupRealtimeDetection() {
        analyzer = new FrameAnalyzer(ObjectDetectorHelper.CLASSIFY_MULTIPLE_OBJECTS, ObjectDetectorOptions.STREAM_MODE);
        analyzer.setView(boundingBox);
        analyzer.setInputResolution(imageAnalysis.getResolutionInfo().getResolution());
        analyzer.setPreviewResolution(new Size(previewView.getWidth(), previewView.getHeight()));

        // Maintain a map of detected objects with a timestamp
        Map<String, Long> activeObjects = new HashMap<>();
        final long TIMEOUT_MS = 2000; // Time to forget undetected objects

        analyzer.setObjectDetectionListener(objects -> {
            long currentTime = System.currentTimeMillis();
            Set<String> currentCycleObjects = new HashSet<>();

            if (objects != null && !objects.isEmpty() && ttsActivado) {
                for (FrameAnalyzer.DetectedObjectData data : objects) {
                    String translatedLabel = translationMap.getOrDefault(data.objectName.toLowerCase(), data.objectName);
                    String uniqueKey = translatedLabel + "-" + data.position;

                    // Add to current cycle's detected objects
                    currentCycleObjects.add(uniqueKey);

                    // Speak only if not recently announced
                    if (!activeObjects.containsKey(uniqueKey)) {
                        speak(translatedLabel + " " + data.position);
                        activeObjects.put(uniqueKey, currentTime);
                    } else {
                        // Update the timestamp for the object still being detected
                        activeObjects.put(uniqueKey, currentTime);
                    }
                }
            }

            // Remove objects that are no longer detected
            activeObjects.keySet().removeIf(key -> !currentCycleObjects.contains(key)
                    && (currentTime - activeObjects.get(key)) > TIMEOUT_MS);
        });

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> analyzer.analyze(image));
    }

    private void inicializarTTS() {
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
        if (ttsActivado && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    private void processQueue() {
        if (!isSpeaking && !ttsQueue.isEmpty()) {
            String message = ttsQueue.poll();
            if (message != null) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, message);
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (analyzer != null) {
            analyzer.closeDetector();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}