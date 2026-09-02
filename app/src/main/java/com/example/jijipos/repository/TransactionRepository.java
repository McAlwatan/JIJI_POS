package com.example.jijipos.repository;

import android.content.Context;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.dao.TransactionDao;
import com.example.jijipos.database.entity.Transaction;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.transactionDao = db.transactionDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }

    public void insertTransaction(Transaction transaction, RepositoryCallback<Long> callback) {
        executorService.execute(() -> {
            long id = transactionDao.insertTransaction(transaction);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void getBusinessSalesTotal(long businessId, long startTime, long endTime, RepositoryCallback<Double> callback) {
        executorService.execute(() -> {
            double total = transactionDao.getBusinessSalesTotal(businessId, startTime, endTime);
            if (callback != null) {
                callback.onComplete(total);
            }
        });
    }

    public void getCustomerSpendingTotal(long customerId, long startTime, long endTime, RepositoryCallback<Double> callback) {
        executorService.execute(() -> {
            double total = transactionDao.getCustomerSpendingTotal(customerId, startTime, endTime);
            if (callback != null) {
                callback.onComplete(total);
            }
        });
    }
}
