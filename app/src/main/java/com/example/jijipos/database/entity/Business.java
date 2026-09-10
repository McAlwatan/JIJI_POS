package com.example.jijipos.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "business")
public class Business {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String businessName;
    private String location;
    private String phoneNumber;
    private long createdAt;


    public Business() {}

    public Business(String businessName, String location, String phoneNumber, long createdAt){
        this.businessName = businessName;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id;}
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) {this.businessName = businessName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
