package com.example.directionstest.ADAPTERS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.Contactos;
import com.example.directionstest.ROOM.BasedeDatos;
import com.example.directionstest.ROOM.Contact;
import com.example.directionstest.R;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    private List<Contact> contactos;
    private Context context;
    private BasedeDatos db;

    public ContactsAdapter(List<Contact> contactos, Context context) {
        this.contactos = contactos;
        this.context = context;
        this.db = BasedeDatos.getInstance(context);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactos.get(position);
        holder.nombre.setText(contact.nombre);
        holder.celular.setText(contact.numerocel);

        holder.btnFav.setImageDrawable(context.getDrawable(R.drawable.ic_favorite_unmark));

        holder.btnFav.setOnClickListener(v -> {
            new Thread(() -> {
                db.favoriteContactDao().insert(contact);
                holder.btnFav.setImageDrawable(context.getDrawable(R.drawable.ic_favorite));
                ((Contactos) context).loadFavorites(()->notifyDataSetChanged());
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }
    public void setContactos(List<Contact> newContactos) {
        contactos.clear();
        for (Contact newContact : newContactos) {
            if (!isDuplicate(contactos, newContact)) {
                contactos.add(newContact);
            }
        }
        notifyDataSetChanged();
    }

    private boolean isDuplicate(List<Contact> list, Contact contact) {
        for (Contact contacto : list) {
            if (contacto.nombre.equalsIgnoreCase(contact.nombre)
                    && contacto.numerocel.equals(contact.numerocel)) {
                return true;
            }
        }
        return false;
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nombre, celular;
        ImageButton btnFav;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.contact_name);
            celular = itemView.findViewById(R.id.contact_phone);
            btnFav = itemView.findViewById(R.id.favorite_button);
        }
    }
}
