package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.model.UserProfile;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    LiveData<UserProfile> getProfile();

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    UserProfile getProfileSync();
}
