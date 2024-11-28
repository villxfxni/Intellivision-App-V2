package com.example.directionstest.ADAPTERS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.ROOM.BasedeDatos;
import com.example.directionstest.ROOM.FavoritePlace;
import com.example.directionstest.Ubicaciones;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import com.example.directionstest.R;
public class FavoritePlacesAdapter extends RecyclerView.Adapter<FavoritePlacesAdapter.ViewHolder> {

    private final ArrayList<FavoritePlace> places;
    private final LayoutInflater inflater;
    private final OnPlaceClickListener onPlaceClickListener;
    Context context;
    public interface OnPlaceClickListener {
        void onPlaceClick(FavoritePlace place);
    }

    public FavoritePlacesAdapter(Context context, ArrayList<FavoritePlace> places, OnPlaceClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.places = places;
        this.onPlaceClickListener = listener;
        this.context = context;

    }

    @NonNull
    @Override
    public FavoritePlacesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.favorite_place_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritePlacesAdapter.ViewHolder holder, int position) {
        FavoritePlace place = places.get(position);
        holder.name.setText("Nombre: " + place.name);
        holder.address.setText("Dirección: " + place.Direccion);
        holder.latitude.setText("Latitud: " + place.latitude);
        holder.longitude.setText("Longitud: " + place.longitude);

        holder.btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Eliminar Lugar");
            builder.setMessage("¿Estás seguro de que quieres eliminar " + place.name + "?");

            builder.setPositiveButton("Sí", (dialog, which) -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    BasedeDatos database = BasedeDatos.getInstance(context);
                    database.favoritePlaceDao().deleteById(place.id);
                    ((Ubicaciones) context).runOnUiThread(() -> {
                        places.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, place.name + " eliminado.", Toast.LENGTH_SHORT).show();
                    });
                });
            });

            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
        holder.cardView.setOnClickListener(v -> {
            onPlaceClickListener.onPlaceClick(place);
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name, address, latitude, longitude;
        ImageButton btnDelete;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.place_name);
            address = itemView.findViewById(R.id.place_address);
            latitude = itemView.findViewById(R.id.place_latitude);
            longitude = itemView.findViewById(R.id.place_longitude);
            cardView = itemView.findViewById(R.id.place_card);
            btnDelete = itemView.findViewById(R.id.deleteButton);
        }
    }
}
