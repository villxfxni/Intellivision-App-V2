package com.example.directionstest.ROOM;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;


@Database(entities = {Contact.class, FavoritePlace.class}, version = 2)
public abstract class BasedeDatos extends RoomDatabase {
    public abstract ContactDAO favoriteContactDao();
    public abstract FavoritePlaceDao favoritePlaceDao();

    private static volatile BasedeDatos INSTANCE;

    public static BasedeDatos getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BasedeDatos.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    BasedeDatos.class, "favorites_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
