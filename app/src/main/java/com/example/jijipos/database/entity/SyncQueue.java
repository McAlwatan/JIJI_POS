package com.example.jijipos.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_sync_queue")
public class SyncQueue {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long cashierId;
    private long businessId;
    private String actionType;
    private String payloadJson;
    private long createdAt;
    private String syncStatus;

    // Room Mandatory Empty Constructor
    public SyncQueue() {}

    public SyncQueue(long cashierId, long businessId, String actionType, String payloadJson, long createdAt, String syncStatus) {
        this.cashierId = cashierId;
        this.businessId = businessId;
        this.actionType = actionType;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCashierId() { return cashierId; }
    public void setCashierId(long cashierId) { this.cashierId = cashierId; }
    public long getBusinessId() { return businessId; }
    public void setBusinessId(long businessId) { this.businessId = businessId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
