package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.jijipos.R;
import com.example.jijipos.ReceiptAdapter;
import com.example.jijipos.database.AppDatabase;
import java.util.concurrent.Executors;

public class CashierTransactionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cashier_transactions, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerViewTransactions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        Executors.newSingleThreadExecutor().execute(() -> {
            long userId = getActivity().getIntent().getLongExtra("USER_ID", 0L);
            AppDatabase db = AppDatabase.getInstance(getContext());
            var transactions = db.transactionDao().getTransactionsByCashier(userId);
            
            getActivity().runOnUiThread(() -> {
                rv.setAdapter(new ReceiptAdapter(transactions, tx -> {
                    // Show receipt detail or handle click
                }));
            });
        });

        return view;
    }
}
