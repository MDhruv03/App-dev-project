package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.NotificationDao;
import com.example.myapplication.model.Notification;
import com.example.myapplication.util.Logger;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for notifications
 */
public class NotificationViewModel extends AndroidViewModel {
    
    private static final String TAG = "NotificationViewModel";
    
    private final NotificationDao notificationDao;
    private final ExecutorService executorService;
    private final LiveData<List<Notification>> allNotifications;
    private final LiveData<List<Notification>> unreadNotifications;
    private final LiveData<Integer> unreadCount;
    
    public NotificationViewModel(@NonNull Application application) {
        super(application);
        
        AppDatabase database = AppDatabase.getInstance(application);
        notificationDao = database.notificationDao();
        executorService = Executors.newSingleThreadExecutor();
        
        allNotifications = notificationDao.getAllNotifications();
        unreadNotifications = notificationDao.getUnreadNotifications();
        unreadCount = notificationDao.getUnreadCount();
    }
    
    public LiveData<List<Notification>> getAllNotifications() {
        return allNotifications;
    }
    
    public LiveData<List<Notification>> getUnreadNotifications() {
        return unreadNotifications;
    }
    
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }
    
    public LiveData<List<Notification>> getNotificationsByType(String type) {
        return notificationDao.getNotificationsByType(type);
    }
    
    public void createNotification(String type, String title, String message, 
                                   Long relatedId, String actionUrl) {
        executorService.execute(() -> {
            try {
                Notification notification = new Notification();
                notification.setType(type);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setTimestamp(new Date().getTime());
                notification.setRead(false);
                if (relatedId != null) {
                    notification.setRelatedId(relatedId.intValue());
                }
                notification.setActionUrl(actionUrl);
                
                notificationDao.insert(notification);
                Logger.d(TAG, "Notification created: " + title);
            } catch (Exception e) {
                Logger.e(TAG, "Error creating notification", e);
            }
        });
    }
    
    public void markAsRead(long notificationId) {
        executorService.execute(() -> {
            try {
                notificationDao.markAsRead((int) notificationId);
                Logger.d(TAG, "Notification marked as read: " + notificationId);
            } catch (Exception e) {
                Logger.e(TAG, "Error marking notification as read", e);
            }
        });
    }
    
    public void markAllAsRead() {
        executorService.execute(() -> {
            try {
                notificationDao.markAllAsRead();
                Logger.d(TAG, "All notifications marked as read");
            } catch (Exception e) {
                Logger.e(TAG, "Error marking all notifications as read", e);
            }
        });
    }
    
    public void deleteNotification(long notificationId) {
        executorService.execute(() -> {
            try {
                Notification notification = new Notification();
                notification.setId((int) notificationId);
                notificationDao.delete(notification);
                Logger.d(TAG, "Notification deleted: " + notificationId);
            } catch (Exception e) {
                Logger.e(TAG, "Error deleting notification", e);
            }
        });
    }
    
    public void deleteOldNotifications(Date beforeDate) {
        executorService.execute(() -> {
            try {
                notificationDao.deleteOldNotifications(beforeDate.getTime());
                Logger.d(TAG, "Old notifications deleted");
            } catch (Exception e) {
                Logger.e(TAG, "Error deleting old notifications", e);
            }
        });
    }
    
    public void clearAllNotifications() {
        executorService.execute(() -> {
            try {
                notificationDao.deleteAll();
                Logger.d(TAG, "All notifications cleared");
            } catch (Exception e) {
                Logger.e(TAG, "Error clearing notifications", e);
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
