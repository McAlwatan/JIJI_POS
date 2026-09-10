package com.example.jijipos.database.entity;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        foreignKeys = {
                @ForeignKey(entity = Business.class, parentColumns = "id", childColumns = "businessId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "cashierId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("businessId"), @Index("cashierId")}
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long businessId;
    private long customerId;
    private long cashierId;
    private double totalAmount;
    private String category; // hapa things like ( "Utilities", "Shopping", "Personal" )
    private long timestamp;
    private boolean isRefunded;

    public Transaction() {}

    public Transaction(long businessId, long customerId, long cashierId, double totalAmount, String category, long timestamp, boolean isRefunded){
        this.businessId = businessId;
        this.cashierId = cashierId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.category = category;
        this.timestamp = timestamp;
        this.isRefunded = isRefunded;
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBusinessId() { return businessId; }
    public void setBusinessId(long businessId) { this.businessId = businessId; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public long getCashierId() { return cashierId; }
    public void setCashierId(long cashierId) { this.cashierId = cashierId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRefunded() { return isRefunded; }
    public void setRefunded(boolean refunded) { isRefunded = refunded; }

}
