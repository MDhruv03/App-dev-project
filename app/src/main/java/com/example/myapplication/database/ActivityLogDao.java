package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.model.ActivityLog;

import java.util.Date;
import java.util.List;

/**
 * DAO for ActivityLog operations
 */
@Dao
public interface ActivityLogDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ActivityLog activityLog);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ActivityLog> activityLogs);
    
    @Delete
    void delete(ActivityLog activityLog);
    
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ActivityLog>> getRecentActivities(int limit);
    
    @Query("SELECT * FROM activity_logs WHERE activityType = :type ORDER BY timestamp DESC")
    LiveData<List<ActivityLog>> getActivitiesByType(String type);
    
    @Query("SELECT * FROM activity_logs WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    LiveData<List<ActivityLog>> getActivitiesInRange(Date startDate, Date endDate);
    
    @Query("SELECT * FROM activity_logs WHERE relatedOpportunityId = :opportunityId ORDER BY timestamp DESC")
    LiveData<List<ActivityLog>> getActivitiesForOpportunity(int opportunityId);
    
    @Query("DELETE FROM activity_logs WHERE timestamp < :timestampBefore")
    void deleteOldActivities(Date timestampBefore);
    
    @Query("SELECT COUNT(*) FROM activity_logs")
    LiveData<Integer> getTotalActivityCount();
    
    @Query("DELETE FROM activity_logs")
    void deleteAll();
}
