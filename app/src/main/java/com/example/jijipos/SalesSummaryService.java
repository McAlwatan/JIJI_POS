package com.example.jijipos;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.jijipos.repository.TransactionRepository;

public class SalesSummaryService extends Service {
    private static final String TAG = "SalesSummaryService";
    private TransactionRepository transactionRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        transactionRepository = new TransactionRepository(this);
        Log.d(TAG, "Sales summary tracking service initialized.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long businessId = intent.getLongExtra("BUSINESS_ID", -1);
        Log.d(TAG, "Processing daily totals for business: " + businessId);

        if (businessId != -1) {
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (24 * 60 * 60 * 1000); // Lookback exactly 24 hours

            // Safely execute the repository calculation query on background threads
            transactionRepository.getBusinessSalesTotal(businessId, startTime, endTime, total -> {
                // Return execution context payload back to a system log or notification trigger
                Log.d(TAG, "Background Calculation Complete. 24hr Sales Volume Total: TZS " + total);

                // Stop the service automatically once the task is finished to preserve battery
                stopSelf();
            });
        } else {
            stopSelf();
        }

        return START_NOT_STICKY; // Do not auto-restart if the OS kills it for memory
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We are using a Started Service, not a Bound Service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Sales summary tracking service stopped.");
    }
}
