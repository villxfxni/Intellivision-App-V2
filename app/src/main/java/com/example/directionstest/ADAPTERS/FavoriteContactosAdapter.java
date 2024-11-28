package com.example.directionstest.ADAPTERS;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.Contactos;
import com.example.directionstest.ROOM.BasedeDatos;
import com.example.directionstest.ROOM.Contact;
import com.example.directionstest.R;

import java.util.List;

public class FavoriteContactosAdapter extends RecyclerView.Adapter<FavoriteContactosAdapter.FavoriteViewHolder> {
    private List<Contact> favorites;
    private Context context;
    private BasedeDatos db;

    public FavoriteContactosAdapter(List<Contact> favorites, Context context) {
        this.favorites = favorites;
        this.context = context;
        this.db = BasedeDatos.getInstance(context);
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Contact contact = favorites.get(position);
        holder.name.setText(contact.nombre);
        holder.phone.setText(contact.numerocel);

        holder.favoriteButton.setImageDrawable(context.getDrawable(R.drawable.ic_favorite));

        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("ALERTA ")
                    .setMessage("Llamar a " + contact.nombre + "?")
                    .setPositiveButton("LLAMAR", (dialog, which) -> {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + contact.numerocel));

                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            context.startActivity(callIntent);
                        } else {
                            Toast.makeText(context, "No se tiene el permiso de llamada", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("SALIR", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        holder.favoriteButton.setOnClickListener(v -> {
            new Thread(() -> {
                db.favoriteContactDao().delete(contact);
                ((Contactos) context).loadFavorites(()->notifyDataSetChanged());
            }).start();
        });
    }

    public void setFavorites(List<Contact> newFavorites) {
        favorites.clear();
        for (Contact newContact : newFavorites) {
            if (!isDuplicate(favorites, newContact)) {
                favorites.add(newContact);
            }
        }
        notifyDataSetChanged();
    }

    private boolean isDuplicate(List<Contact> list, Contact contact) {
        for (Contact existingContact : list) {
            if (existingContact.nombre.equalsIgnoreCase(contact.nombre)
                    && existingContact.numerocel.equals(contact.numerocel)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView name, phone;
        ImageButton favoriteButton;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            phone = itemView.findViewById(R.id.contact_phone);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
}
