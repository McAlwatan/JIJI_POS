package com.example.jijipos.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "local_bill_splits",
        foreignKeys = {
                @ForeignKey(entity = Transaction.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "creatorCustomerId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("transactionId"), @Index("creatorCustomerId")}
)
public class BillSplit {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long transactionId;
    private long creatorCustomerId;
    private int totalWaysSplit;
    private double amountPerPerson;
    private String status;

    // Room Mandatory Empty Constructor
    public BillSplit() {}

    public BillSplit(long transactionId, long creatorCustomerId, int totalWaysSplit, double amountPerPerson, String status) {
        this.transactionId = transactionId;
        this.creatorCustomerId = creatorCustomerId;
        this.totalWaysSplit = totalWaysSplit;
        this.amountPerPerson = amountPerPerson;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
    public long getCreatorCustomerId() { return creatorCustomerId; }
    public void setCreatorCustomerId(long creatorCustomerId) { this.creatorCustomerId = creatorCustomerId; }
    public int getTotalWaysSplit() { return totalWaysSplit; }
    public void setTotalWaysSplit(int totalWaysSplit) { this.totalWaysSplit = totalWaysSplit; }
    public double getAmountPerPerson() { return amountPerPerson; }
    public void setAmountPerPerson(double amountPerPerson) { this.amountPerPerson = amountPerPerson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
