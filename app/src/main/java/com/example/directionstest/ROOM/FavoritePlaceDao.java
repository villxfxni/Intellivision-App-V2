package com.example.directionstest.ROOM;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoritePlaceDao {
    @Insert
    void insertFavoritePlace(FavoritePlace place);

    @Query("SELECT * FROM favorite_places")
    List<FavoritePlace> getAllFavoritePlaces();

    @Delete
    void deleteFavoritePlace(FavoritePlace place);
    @Query("DELETE FROM favorite_places WHERE id = :id")
    void deleteById(int id);
}
