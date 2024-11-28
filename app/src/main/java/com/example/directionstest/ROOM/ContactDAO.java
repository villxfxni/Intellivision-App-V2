package com.example.directionstest.ROOM;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface ContactDAO {
    @Insert
    void insert(Contact contact);

    @Query("SELECT * FROM favoritos")
    List<Contact> getAllFavorites();

    @Query("SELECT * FROM favoritos WHERE id = :id LIMIT 1")
    Contact getFavoriteById(int id);

    @Delete
    void delete(Contact contact);
}
