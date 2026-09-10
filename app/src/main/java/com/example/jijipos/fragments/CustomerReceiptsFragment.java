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

import com.example.jijipos.R;
import com.example.jijipos.ReceiptAdapter;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Transaction;

import java.util.List;
import java.util.concurrent.Executors;

public class CustomerReceiptsFragment extends Fragment {

    private RecyclerView recyclerViewReceipts;
    private TextView textNoReceiptsHint;
    private long activeCustomerId = 99L; // Using our matching mock Customer ID from the scanner step

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_receipts, container, false);

        recyclerViewReceipts = view.findViewById(R.id.recyclerViewReceipts);
        textNoReceiptsHint = view.findViewById(R.id.textNoReceiptsHint); // We will add this small fallback to the XML

        recyclerViewReceipts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Call the live background database extraction routine
        loadLiveReceiptHistory();
        return view;
    }

    private void loadLiveReceiptHistory() {
        // Offload the database query entirely to a clean background thread pool
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            // Query the transaction table for all records matching this specific customer ID
            final List savedReceipts = db.transactionDao().getReceiptHistoryByCustomer(99L);
            // Note: In Month 2, we will add an explicit query filter inside TransactionDao targeting customerId directly!

            if (getActivity() != null) {
                // Bounce back onto the main UI thread to update your display widgets safely
                getActivity().runOnUiThread(() -> {
                    if (savedReceipts == null || savedReceipts.isEmpty()) {
                        // If database is clean and empty, show a friendly instruction hint
                        textNoReceiptsHint.setVisibility(View.VISIBLE);
                        recyclerViewReceipts.setVisibility(View.GONE);
                    } else {
                        // If data is found, plug it directly into your visual adapter matrix row items
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
