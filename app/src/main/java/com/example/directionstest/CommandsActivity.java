package com.example.directionstest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.ADAPTERS.CommandsAdapter;
import com.example.directionstest.ENTITY.Command;

import java.util.ArrayList;
import java.util.List;

public class CommandsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_commands);

        RecyclerView recyclerView = findViewById(R.id.commandsRecyclerView);
        List<Command> commands = new ArrayList<>();

        commands.add(new Command("VISION", "Verifica el estado del servicio (verifica volumen y conexión si no escuchas una respuesta).", R.drawable.ic_internet));
        commands.add(new Command("COMANDOS", "Abre la guía de comandos (este menú)", R.drawable.ic_arrow_back));
        commands.add(new Command("CONTACTO", "Abre los contactos favoritos.", R.drawable.ic_contacts));
        commands.add(new Command("LLAMAR", "Ejemplo: LLAMAR PEPE.", R.drawable.ic_call));
        commands.add(new Command("RUTA", "Abre la pantalla de rutas.", R.drawable.ic_route));
        commands.add(new Command("IR A", "Ejemplo: IR A CASA.", R.drawable.ic_map));
        commands.add(new Command("GUARDAR LUGAR", "Almacena el punto marcado en el mapa", R.drawable.ic_map));
        commands.add(new Command("ELIMINAR LUGAR", "Elimina el punto marcado en el mapa", R.drawable.ic_map));
        commands.add(new Command("CAMARA", "Abre la detección de objetos.", R.drawable.ic_camera));
        commands.add(new Command("VISION STOP", "Cierra la actividad actual.", R.drawable.ic_stop));

        CommandsAdapter adapter = new CommandsAdapter(commands);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
}