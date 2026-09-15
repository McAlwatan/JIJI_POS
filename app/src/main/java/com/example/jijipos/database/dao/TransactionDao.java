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
    // CHANGED TO Double TO SAFELY HANDLE NULL VALUES WHEN DATABASE IS NEW
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE businessId = :businessId AND isRefunded = 0 AND timestamp BETWEEN :startTime AND :endTime")
    Double getBusinessSalesTotal(long businessId, long startTime, long endTime);

    // Month 2 Core: Aggregates personal spending totals for a standalone Customer account
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE customerId = :customerId AND isRefunded = 0 AND timestamp BETWEEN :startTime AND :endTime")
    double getCustomerSpendingTotal(long customerId, long startTime, long endTime);

    @Query("DELETE FROM transactions WHERE timestamp < :cutoffTimestamp")
    int pruneOldLocalHistory(long cutoffTimestamp);

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    List<Transaction> getReceiptHistoryByCustomer(long customerId);

    // Place this directly inside your TransactionDao.java interface source file
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE customerId = :customerId AND timestamp >= :startTime AND timestamp <= :endTime")
    Double getCustomerExpensesSum(long customerId, long startTime, long endTime);

    // Fetching the total amount for the cashier's personal sales
    @Query("SELECT SUM(totalAmount) FROM transactions WHERE cashierId = :cashierId AND timestamp >= :startTime AND timestamp <= :endTime")
    Double getPersonalCashierSalesTotal(long cashierId, long startTime, long endTime);

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE businessId = :businessId AND timestamp >= :startTime AND timestamp <= :endTime")
    Double getManagerEnterpriseSalesTotal(long businessId, long startTime, long endTime);
}
