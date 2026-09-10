package com.example.jijipos.database.entity;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        foreignKeys = @ForeignKey(
                entity = Business.class,
                parentColumns = "id",
                childColumns = "businessId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {@Index("businessId")}
)

public class User {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String fullName;
    private String phoneNumber;

    private String passwordHash;

    private String role;
    private Long businessId;

    public User() {}


    public User(String fullName, String phoneNumber, String passwordHash, String role, Long businessId){
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.role = role;
        this.businessId = businessId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role;}
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
}
