package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.model.PendingSyncOperation;

import java.util.List;

@Dao
public interface PendingSyncOperationDao {

    @Insert
    long insert(PendingSyncOperation operation);

    @Query("SELECT * FROM pending_sync_operations ORDER BY createdAt ASC LIMIT :limit")
    List<PendingSyncOperation> getPendingOperations(int limit);

    @Query("DELETE FROM pending_sync_operations WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE pending_sync_operations SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    void markRetryFailed(int id, String error);

    @Query("DELETE FROM pending_sync_operations WHERE retryCount >= :maxRetries")
    void deleteExhaustedRetries(int maxRetries);

    @Query("SELECT COUNT(*) FROM pending_sync_operations")
    int getTotalPendingCount();

    @Query("SELECT COUNT(*) FROM pending_sync_operations WHERE entityType = :entityType")
    int getPendingCountByEntityType(String entityType);

    @Query("SELECT COUNT(*) FROM pending_sync_operations WHERE entityType = :entityType AND operationType = :operationType")
    int getPendingCountByEntityAndOperation(String entityType, String operationType);
}
