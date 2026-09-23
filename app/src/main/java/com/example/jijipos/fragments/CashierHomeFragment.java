package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.jijipos.R;

public class CashierHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cashier_home, container, false);

        LinearLayout btnNewSale = view.findViewById(R.id.btnNewSale);
        LinearLayout actionHistory = view.findViewById(R.id.actionHistory);
        LinearLayout actionRefund = view.findViewById(R.id.actionRefund);
        LinearLayout actionSyncQueue = view.findViewById(R.id.actionSyncQueue);

        btnNewSale.setOnClickListener(v -> navigateToSales());
        actionHistory.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CashierTransactionsFragment())
                    .addToBackStack(null)
                    .commit();
        });
        actionRefund.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new RefundFragment())
                    .addToBackStack(null)
                    .commit();
        });
        actionSyncQueue.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SyncQueueFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void navigateToSales() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new SalesFragment())
                .addToBackStack(null)
                .commit();
    }
}
