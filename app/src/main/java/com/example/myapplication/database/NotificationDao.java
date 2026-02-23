package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Notification;

import java.util.List;

/**
 * DAO for Notification operations
 */
@Dao
public interface NotificationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Notification notification);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Notification> notifications);
    
    @Update
    void update(Notification notification);
    
    @Delete
    void delete(Notification notification);
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<Notification>> getAllNotifications();
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<Notification>> getUnreadNotifications();
    
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<Notification>> getNotificationsByType(String type);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    LiveData<Integer> getUnreadCount();
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);
    
    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();
    
    @Query("DELETE FROM notifications WHERE timestamp < :timestampBefore")
    void deleteOldNotifications(long timestampBefore);
    
    @Query("DELETE FROM notifications")
    void deleteAll();
}
