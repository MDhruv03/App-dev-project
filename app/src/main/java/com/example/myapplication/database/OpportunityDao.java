package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Opportunity;

import java.util.List;

@Dao
public interface OpportunityDao {
    
    @Insert
    long insert(Opportunity opportunity);
    
    @Update
    void update(Opportunity opportunity);
    
    @Delete
    void delete(Opportunity opportunity);
    
    @Query("SELECT * FROM opportunities ORDER BY recommendationScore DESC")
    List<Opportunity> getAllOpportunities();
    
    @Query("SELECT * FROM opportunities WHERE isSaved = 1 ORDER BY updatedAt DESC")
    List<Opportunity> getSavedOpportunities();
    
    @Query("SELECT * FROM opportunities WHERE type = :type ORDER BY recommendationScore DESC")
    List<Opportunity> getOpportunitiesByType(String type);
    
    @Query("SELECT * FROM opportunities WHERE recommendationScore >= 70 ORDER BY recommendationScore DESC LIMIT 10")
    List<Opportunity> getRecommendedOpportunities();
    
    @Query("SELECT * FROM opportunities WHERE id = :id")
    Opportunity getOpportunityById(int id);
    
    @Query("UPDATE opportunities SET isSaved = :saved WHERE id = :id")
    void updateSavedStatus(int id, boolean saved);
    
    @Query("UPDATE opportunities SET isApplied = :applied WHERE id = :id")
    void updateAppliedStatus(int id, boolean applied);
    
    @Query("DELETE FROM opportunities")
    void deleteAll();
}
