package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;

@Entity(tableName = "pending_sync_operations")
@TypeConverters(Converters.class)
public class PendingSyncOperation {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String entityType;
    private String operationType;
    private String payloadJson;
    private Date createdAt;
    private int retryCount;
    private String lastError;

    public PendingSyncOperation() {
        this.createdAt = new Date();
        this.retryCount = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
