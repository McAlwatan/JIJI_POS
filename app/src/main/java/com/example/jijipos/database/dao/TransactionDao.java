package com.example.jijipos.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.jijipos.database.entity.Transaction;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insertTransaction(Transaction transaction);

    // Month 3 Module: Update transaction when handling a refund
    @Update
    void updateTransaction(Transaction transaction);

    // Fetch all transactions for a business (ordered by newest first)
    @Query("SELECT * FROM transactions WHERE businessId = :businessId ORDER BY timestamp DESC")
    List<Transaction> getAllTransactionsForBusiness(long businessId);

    // Month 2 Core: Aggregates total sales for a business within a specific timestamp window
    // This allows you to track sales by day, week, month, or year
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE businessId = :businessId AND isRefunded = 0 AND timestamp BETWEEN :startTime AND :endTime")
    double getBusinessSalesTotal(long businessId, long startTime, long endTime);

    // Month 2 Core: Aggregates personal spending totals for a standalone Customer account
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE customerId = :customerId AND isRefunded = 0 AND timestamp BETWEEN :startTime AND :endTime")
    double getCustomerSpendingTotal(long customerId, long startTime, long endTime);
}
