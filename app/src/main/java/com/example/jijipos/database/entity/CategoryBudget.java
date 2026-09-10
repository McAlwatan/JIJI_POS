package com.example.jijipos.database.entity;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "local_budget",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "customerId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("CustomerId")}
)
public class CategoryBudget {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long customerId;
    private String categoryName;
    private double allocatedBudget;
    private double spentAmount;
    private String timeWindow;

    public CategoryBudget() {}

    public CategoryBudget(long customerId, String categoryName, double allocatedBudget, double spentAmount, String timeWindow){
        this.customerId = customerId;
        this.categoryName = categoryName;
        this.allocatedBudget = allocatedBudget;
        this.timeWindow = timeWindow;
        this.spentAmount = spentAmount;
    }

    public long getId() {return id;}
    public void setId(long id) { this.id = id; }
    public long getCustomerId() {return customerId; }
    public void setCustomerId(long customerId) {this.customerId = customerId; }
    public String getCategoryName() {return categoryName; }
    public void setCategoryName(String categoryName) {this.categoryName = categoryName; }
    public double getAllocatedBudget() {return allocatedBudget; }
    public void setAllocatedBudget(double allocatedBudget) { this.allocatedBudget = allocatedBudget; }
    public double getSpentAmount() {return spentAmount; }
    public void setSpentAmount(double spentAmount) {this.spentAmount = spentAmount; }
    public String getTimeWindow() {return timeWindow; }
    public void setTimeWindow(String timeWindow) {this.timeWindow = timeWindow; }
}
