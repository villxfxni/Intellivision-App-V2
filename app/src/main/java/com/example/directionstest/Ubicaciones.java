package com.example.directionstest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.ADAPTERS.FavoritePlacesAdapter;
import com.example.directionstest.ROOM.BasedeDatos;
import com.example.directionstest.ROOM.FavoritePlace;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class   Ubicaciones extends AppCompatActivity implements OnMapReadyCallback {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private TextToSpeech tts;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private BasedeDatos database;
    private RecyclerView recyclerView;
    private FavoritePlacesAdapter adapter;
    private ArrayList<FavoritePlace> favoritePlaces = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ubicaciones);

        initializeTextToSpeech();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        database = BasedeDatos.getInstance(this);

        recyclerView = findViewById(R.id.favorites_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FavoritePlacesAdapter(this, favoritePlaces, place -> {
            Toast.makeText(this, "Seleccionaste: " + place.name, Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnGuardarUbi).setOnClickListener(v -> saveCurrentLocation());

        findViewById(R.id.btnGoogleComms).setOnClickListener(v -> voiceInput());
        ImageButton btn = findViewById(R.id.btnBack);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        cargarLugaresFavoritos();
    }

    private void saveCurrentLocation() {
        if (googleMap != null) {
            LatLng currentLatLng = googleMap.getCameraPosition().target;
            String address = direccionCoordenadas(currentLatLng.latitude, currentLatLng.longitude);

            showSaveDialog(currentLatLng, address);
        }
    }

    private void showSaveDialog(LatLng latLng, String address) {
        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Ubicación seleccionada")
                    .snippet(address));
        }

        speak("¿Quieres guardar esta ubicación en la dirección: " + address + "?");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar ubicación");
        builder.setMessage("¿Quieres guardar esta ubicación?\nDirección: " + address);

        builder.setPositiveButton("Sí", (dialog, which) -> procesarNombreUbicacion(latLng, address));

        builder.setNegativeButton("No", (dialog, which) -> {
            speak("Ubicación descartada.");
            dialog.dismiss();
        });

        builder.show();
    }
    private void procesarNombreUbicacion(LatLng latLng, String address) {
        speak("Por favor, di o escribe el nombre del lugar.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nombre del lugar");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Ingresa el nombre del lugar");
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String placeName = input.getText().toString().trim();
            if (!placeName.isEmpty()) {
                guardarLugarFavorito(placeName, latLng.latitude, latLng.longitude, address);
            } else {
                speak("El nombre del lugar no puede estar vacío.");
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void guardarLugarFavorito(String nombre, double latitude, double longitude, String address) {
        FavoritePlace place = new FavoritePlace();
        place.name = nombre;
        place.latitude = latitude;
        place.longitude = longitude;
        place.Direccion = address;

        Executors.newSingleThreadExecutor().execute(() -> {
            database.favoritePlaceDao().insertFavoritePlace(place);
            cargarLugaresFavoritos();
        });
    }

    private void cargarLugaresFavoritos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FavoritePlace> placesFromDb = database.favoritePlaceDao().getAllFavoritePlaces();

            runOnUiThread(() -> {
                favoritePlaces.clear();
                favoritePlaces.addAll(placesFromDb);
                adapter.notifyDataSetChanged();
                mostrarMarcadores();
            });
        });
    }

    private void mostrarMarcadores() {
        if (googleMap == null) return;

        googleMap.clear();

        for (FavoritePlace place : favoritePlaces) {
            LatLng location = new LatLng(place.latitude, place.longitude);
            googleMap.addMarker(new MarkerOptions().position(location).title(place.name).snippet(place.Direccion));
        }
    }

    private String direccionCoordenadas(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Dirección desconocida";
    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("es", "MX"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "El idioma no está soportado.");
                }
            } else {
                Log.e("TTS", "Inicialización fallida.");
            }
        });
    }

    private void voiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    private void handleVoiceCommand(String command) {
        if (command.contains("guardar lugar")) {
            String[] parts = command.split("guardar lugar");
            if (parts.length > 1) {
                guardarUbicacionActual(parts[1].trim());
            } else {
                speak("Por favor, di el nombre del lugar que deseas guardar.");
            }
        } else if (command.contains("eliminar lugar")) {
            String[] parts = command.split("eliminar lugar");
            if (parts.length > 1) {
                eliminarLugarFavorito(parts[1].trim());
            } else {
                speak("Por favor, di el nombre del lugar que deseas eliminar.");
            }
        } else if (command.contains("ir a")) {
            String[] parts = command.split("ir a");
            if (parts.length > 1) {
                iniciarRuta(parts[1].trim());
            } else {
                speak("Por favor, di el nombre del lugar al que deseas ir.");
            }
        } else {
            speak("Comando no reconocido. Por favor intenta de nuevo.");
        }
    }
    private void guardarUbicacionActual(String placeName) {
        if (googleMap != null) {
            LatLng currentLatLng = googleMap.getCameraPosition().target;
            String address = direccionCoordenadas(currentLatLng.latitude, currentLatLng.longitude);

            if (placeName == null || placeName.trim().isEmpty()) {
                speak("El nombre del lugar no puede estar vacío.");
                return;
            }

            guardarLugarFavorito(placeName, currentLatLng.latitude, currentLatLng.longitude, address);
            speak("Lugar guardado con el nombre: " + placeName);
        } else {
            speak("No se pudo obtener la ubicación actual.");
        }
    }

    private void eliminarLugarFavorito(String name) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FavoritePlace> placesFromDb = database.favoritePlaceDao().getAllFavoritePlaces();
            for (FavoritePlace place : placesFromDb) {
                if (place.name.equalsIgnoreCase(name)) {
                    database.favoritePlaceDao().deleteFavoritePlace(place);
                    cargarLugaresFavoritos();
                    speak("Lugar eliminado: " + name);
                    return;
                }
            }
            runOnUiThread(() -> speak("No se encontró un lugar con el nombre: " + name));
        });
    }

    private void iniciarRuta(String name) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FavoritePlace> placesFromDb = database.favoritePlaceDao().getAllFavoritePlaces();
            for (FavoritePlace place : placesFromDb) {
                if (place.name.equalsIgnoreCase(name)) {
                    runOnUiThread(() -> startGoogleMapsNavigation(place.name, place.latitude, place.longitude));
                    return;
                }
            }
            runOnUiThread(() -> speak("No se encontró un lugar con el nombre: " + name));
        });
    }

    private void startGoogleMapsNavigation(String placeName, double latitude, double longitude) {
        try {
            String uri = "google.navigation:q=" + latitude + "," + longitude + "&mode=d";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);

                new Handler().postDelayed(() -> {
                    Intent backToApp = new Intent(this, Ubicaciones.class);
                    backToApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(backToApp);
                }, 15000);
            } else {
                speak("Google Maps no está instalado en este dispositivo.");
            }
        } catch (Exception e) {
            Log.e("GoogleMaps", "Error al iniciar Google Maps", e);
            speak("Ocurrió un error al intentar abrir Google Maps.");
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        googleMap.setMyLocationEnabled(true);
        posicionarCamara();
        mostrarMarcadores();
        googleMap.setOnMapClickListener(latLng -> {
            String address = direccionCoordenadas(latLng.latitude, latLng.longitude);

            showSaveDialog(latLng, address);
        });
    }

    private void posicionarCamara() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            } else {
                speak("No se pudo obtener la ubicación actual.");
            }
        });
    }



    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                handleVoiceCommand(results.get(0).toLowerCase());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }
}