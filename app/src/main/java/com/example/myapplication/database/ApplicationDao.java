package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Application;

import java.util.List;

@Dao
public interface ApplicationDao {
    
    @Insert
    long insert(Application application);
    
    @Update
    void update(Application application);
    
    @Delete
    void delete(Application application);
    
    @Query("SELECT * FROM applications WHERE userId = :userId ORDER BY statusUpdatedAt DESC")
    List<Application> getAllApplications(int userId);
    
    @Query("SELECT * FROM applications WHERE userId = :userId AND status = :status ORDER BY statusUpdatedAt DESC")
    List<Application> getApplicationsByStatus(int userId, String status);
    
    @Query("SELECT * FROM applications WHERE id = :id")
    Application getApplicationById(int id);
    
    @Query("SELECT COUNT(*) FROM applications WHERE userId = :userId")
    int getTotalApplications(int userId);
    
    @Query("SELECT COUNT(*) FROM applications WHERE userId = :userId AND status = :status")
    int getApplicationCountByStatus(int userId, String status);
    
    @Query("DELETE FROM applications WHERE userId = :userId")
    void deleteAllForUser(int userId);
}
