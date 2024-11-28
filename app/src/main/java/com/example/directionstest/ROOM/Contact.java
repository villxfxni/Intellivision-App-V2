package com.example.directionstest.ROOM;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favoritos")
public class Contact {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public String numerocel;

    public Contact(String nombre, String numerocel) {
        this.nombre = nombre;
        this.numerocel = numerocel;
    }
}
