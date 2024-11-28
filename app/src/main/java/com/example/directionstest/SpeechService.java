package com.example.directionstest;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.directionstest.DETECTION.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechService extends Service implements RecognitionListener {
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isListening = false;
    private TextToSpeech textToSpeech;
    private boolean isTtsEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        Log.d("STARTED", "CREATED");
        initializeTextToSpeech();
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

    private void startListening() {
        Log.d("STARTED", "STARTED LISTENING33");

        if (!isListening) {
            isListening = true;
            speechRecognizer.startListening(speechIntent);
            Log.d("STARTED", "STARTED LISTENING");
        }
    }

    private void stopListening() {
        isListening = false;
        speechRecognizer.stopListening();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startListening();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
        Log.e("SpeechService", "Error occurred: " + error);
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK:
                Log.e("SpeechService", "Network error");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.e("SpeechService", "No match found");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.e("SpeechService", "Speech timeout");
                break;
            default:
                Log.e("SpeechService", "Unhandled error: " + error);
        }
        stopListening();
        startListening();
    }


    @Override
    public void onResults(Bundle results) {
        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (data != null && !data.isEmpty()) {
            String recognizedText = data.get(0).toLowerCase();
            Log.d("SpeechService", "Recognized Command: " + recognizedText);
            handleCommand(recognizedText);
        } else {
            Log.d("SpeechService", "No results received");
        }
        restartListening();
    }


    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    private void handleCommand(String command) {
        if (command.contains("vision")) {
            if (command.contains("stop")) {
                confirmExit();
            } else {
                respond("Hola, que necesitas?");
                Log.d("STARTED", "CONFIRMACION");
            }
        } else if (command.contains("camera")) {
            openActivity(MainActivity.class);
        } else if (command.contains("ruta")) {
            openActivity(Ubicaciones.class);
        } else if (command.contains("contacto")) {
            openActivity(Contactos.class);
        }else if (command.contains("commandos")) {
            openActivity(CommandsActivity.class);
        }
        else if (command.contains("llamar")) {
            String[] parts = command.split("llamar", 2);
            if (parts.length > 1) {
                String contactName = parts[1].trim();
                if (!contactName.isEmpty()) {
                    Intent intent = new Intent(this, Contactos.class);
                    intent.putExtra("CONTACT_NAME", contactName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Por favor, especifica un nombre de contacto.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Comando de llamada no reconocido. Usa 'llamar [nombre]'.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void respond(String message) {
        if (isTtsEnabled) {
            speak(message);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void confirmExit() {
        Intent intent = new Intent(this, ExitConfirmation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void restartListening() {
        stopListening();
        new android.os.Handler().postDelayed(this::startListening, 2500);
    }

    private void initializeTextToSpeech() {
        if (textToSpeech == null) {
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
    }

    private void speak (String text){
        if (isTtsEnabled && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}
