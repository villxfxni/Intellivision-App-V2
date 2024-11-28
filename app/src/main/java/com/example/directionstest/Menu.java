package com.example.directionstest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.directionstest.DETECTION.MainActivity;

import java.util.ArrayList;

public class Menu extends AppCompatActivity {
private Button btnCamara, btnPlace, btnContact, btnListadoComandos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        btnCamara=findViewById(R.id.btnCam);
        btnContact=findViewById(R.id.btnContacto);
        btnPlace=findViewById(R.id.btnPlace);
        btnListadoComandos=findViewById(R.id.btnListadoComandos);

        if(ContextCompat.checkSelfPermission(Menu.this, android.Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(Menu.this ,new String[]{Manifest.permission.RECORD_AUDIO},1 );
        }
        else {
            Intent serviceIntent = new Intent(this, SpeechService.class);
            startService(serviceIntent);
        }
        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, Ubicaciones.class);
                Menu.this.startActivity(intent);

            }
        });
        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, MainActivity.class);
                Menu.this.startActivity(intent);

            }
        });
        btnContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, Contactos.class);
                Menu.this.startActivity(intent);

            }
        });

        btnListadoComandos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, CommandsActivity.class);
                Menu.this.startActivity(intent);

            }
        });

    }
}