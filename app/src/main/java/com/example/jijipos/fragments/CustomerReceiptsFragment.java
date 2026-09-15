package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.jijipos.R;
import com.example.jijipos.ReceiptAdapter;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Transaction;

import java.util.List;
import java.util.concurrent.Executors;

public class CustomerReceiptsFragment extends Fragment {

    private RecyclerView recyclerViewReceipts;
    private TextView textNoReceiptsHint;
    private SwipeRefreshLayout swipeRefreshLayout; // Added pull refresher handle widget reference

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_receipts, container, false);

        recyclerViewReceipts = view.findViewById(R.id.recyclerViewReceipts);
        textNoReceiptsHint = view.findViewById(R.id.textNoReceiptsHint);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        recyclerViewReceipts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Map gesture manual refreshing listener loops
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadLiveReceiptHistory();
        });

        return view;
    }

    // =============================================================
    // LIFECYCLE HOOK: FORCES AUTOMATIC UPDATE WHEN TAB SWAPS FOCUS
    // =============================================================
    @Override
    public void onResume() {
        super.onResume();
        loadLiveReceiptHistory(); // Automatically query database the instant this screen becomes active
    }
    // =============================================================

    private void loadLiveReceiptHistory() {
        // Offload the database query entirely to a clean background thread pool lane safely
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            // Query the transaction table explicitly for all records matching our test customer ID
            final List<Transaction> savedReceipts = db.transactionDao().getReceiptHistoryByCustomer(99L);

            if (getActivity() != null) {
                // Bounce back onto the main UI thread to update your display widgets safely
                getActivity().runOnUiThread(() -> {
                    // Turn off the spinning loading animation tracker widget indicator frame
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    if (savedReceipts == null || savedReceipts.isEmpty()) {
                        textNoReceiptsHint.setVisibility(View.VISIBLE);
                        recyclerViewReceipts.setVisibility(View.GONE);
                    } else {
                        textNoReceiptsHint.setVisibility(View.GONE);
                        recyclerViewReceipts.setVisibility(View.VISIBLE);

                        ReceiptAdapter adapter = new ReceiptAdapter(savedReceipts);
                        recyclerViewReceipts.setAdapter(adapter);
                    }
                });
            }
        });
    }
}
