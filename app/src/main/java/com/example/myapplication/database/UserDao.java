package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.User;
import com.example.myapplication.model.UserPreferences;

@Dao
public interface UserDao {
    
    @Insert
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);
    
    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);
    
    @Insert
    long insertPreferences(UserPreferences preferences);
    
    @Update
    void updatePreferences(UserPreferences preferences);
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    UserPreferences getPreferencesByUserId(int userId);
}
