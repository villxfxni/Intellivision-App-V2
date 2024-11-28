package com.example.directionstest;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.ADAPTERS.ContactsAdapter;
import com.example.directionstest.ADAPTERS.FavoriteContactosAdapter;
import com.example.directionstest.ROOM.BasedeDatos;
import com.example.directionstest.ROOM.Contact;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Contactos extends AppCompatActivity {

    private ContactsAdapter allContactsAdapter;
    private FavoriteContactosAdapter favoriteContactsAdapter;
    private List<Contact> allContacts = new ArrayList<>();
    private List<Contact> favoriteContacts = new ArrayList<>();
    private BasedeDatos db;
    private RecyclerView recyclerTodos, recyclerFavs;
    private View allContactsLabel, favoritesLabel;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);

        db = BasedeDatos.getInstance(this);

        recyclerTodos = findViewById(R.id.all_contacts_recycler);
        recyclerFavs = findViewById(R.id.favorites_recycler);
        allContactsLabel = findViewById(R.id.all_contacts_label);
        favoritesLabel = findViewById(R.id.favorites_label);
        allContactsAdapter = new ContactsAdapter(allContacts, this);
        recyclerTodos.setAdapter(allContactsAdapter);
        favoriteContactsAdapter = new FavoriteContactosAdapter(favoriteContacts, this);
        recyclerFavs.setAdapter(favoriteContactsAdapter);

        Button btnSincronizar = findViewById(R.id.btnSync);
        Button btnFavs = findViewById(R.id.btnFavoritosCont);

        btnSincronizar.setOnClickListener(v -> showAllContacts());
        btnFavs.setOnClickListener(v -> showFavorites());

        recyclerTodos.setLayoutManager(new LinearLayoutManager(this));
        recyclerFavs.setLayoutManager(new LinearLayoutManager(this));

        requestPermissions();
        Intent intent = getIntent();
        String contactName = intent.getStringExtra("CONTACT_NAME");

        if (contactName != null && !contactName.isEmpty()) {
            loadFavorites(() -> llamarFavorito(contactName));
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });

    }

   public void llamarFavorito(String name) {
        String normalizedInput = formatoNombre(name);
        Log.d("FAVORITES", favoriteContacts.toString());
        for (Contact contact : favoriteContacts) {
            String normalizedContactName = formatoNombre(contact.nombre);
            Log.d("CONTACTOS", normalizedContactName + " + "+normalizedInput );
            if (normalizedContactName.equals(normalizedInput)) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + contact.numerocel));
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(callIntent);
                    return;
                } else {
                    Toast.makeText(this, "Permiso para realizar llamadas no concedido.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(this, "Contacto no encontrado en favoritos.", Toast.LENGTH_SHORT).show();
    }

    private String formatoNombre(String input) {
        if (input == null) return "";
        String result = input.trim().toLowerCase();
        result = java.text.Normalizer.normalize(result, java.text.Normalizer.Form.NFD);
        result = result.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return result;
    }

    private void requestPermissions() {
        String[] permissions = {
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.CALL_PHONE
        };

        boolean permissionsNeeded = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded = true;
                break;
            }
        }

        if (permissionsNeeded) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            showFavorites();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                syncContacts();
            } else {
                Toast.makeText(this, "Se requieren permisos para realizar esta accion", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void showAllContacts() {
        recyclerTodos.setVisibility(View.VISIBLE);
        allContactsLabel.setVisibility(View.VISIBLE);
        recyclerFavs.setVisibility(View.GONE);
        favoritesLabel.setVisibility(View.GONE);
        syncContacts();
    }

    private void showFavorites() {
        recyclerFavs.setVisibility(View.VISIBLE);
        favoritesLabel.setVisibility(View.VISIBLE);
        recyclerTodos.setVisibility(View.GONE);
        allContactsLabel.setVisibility(View.GONE);
        loadFavorites(()-> favoriteContactsAdapter.notifyDataSetChanged());
    }
    private void syncContacts() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        if (cursor != null) {
            allContacts.clear();
            List<Contact> fetchedContacts = new ArrayList<>();
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                fetchedContacts.add(new Contact(name, phone));
            }
            cursor.close();
            allContactsAdapter.setContactos(fetchedContacts);
            Toast.makeText(this, "Contactos Sincronizados", Toast.LENGTH_SHORT).show();
        }
    }
    public void loadFavorites(Runnable callback) {
        new Thread(() -> {
            favoriteContacts.clear();
            List<Contact> fetchedFavorites = db.favoriteContactDao().getAllFavorites();
            favoriteContacts.addAll(fetchedFavorites);

            runOnUiThread(() -> {
                favoriteContactsAdapter.setFavorites(fetchedFavorites);
                if (callback != null) {
                    callback.run();
                }
            });
        }).start();
    }

}
