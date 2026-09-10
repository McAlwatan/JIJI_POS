package com.example.jijipos.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "manager_notifications")
public class NotificationReport {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long managerId;
    private long businessId;
    private String title;
    private String messageContent;
    private boolean isRead;
    private long timestamp;

    // Room Mandatory Empty Constructor
    public NotificationReport() {}

    public NotificationReport(long managerId, long businessId, String title, String messageContent, boolean isRead, long timestamp) {
        this.managerId = managerId;
        this.businessId = businessId;
        this.title = title;
        this.messageContent = messageContent;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getManagerId() { return managerId; }
    public void setManagerId(long managerId) { this.managerId = managerId; }
    public long getBusinessId() { return businessId; }
    public void setBusinessId(long businessId) { this.businessId = businessId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
