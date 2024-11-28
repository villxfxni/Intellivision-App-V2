package com.example.directionstest;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class ExitConfirmation extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.customdialog, null);

        Button btnPositive = customView.findViewById(R.id.btn_positive);
        Button btnNegative = customView.findViewById(R.id.btn_negative);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(customView)
                .setCancelable(false)
                .create();

        btnPositive.setOnClickListener(v -> {
            stopService(new Intent(this, SpeechService.class));
            finishAffinity();
            System.exit(0);
        });
        btnNegative.setOnClickListener(v -> alertDialog.dismiss());
        alertDialog.show();
    }
}
