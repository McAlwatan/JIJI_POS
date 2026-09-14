package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

        ImageButton btnNewSale = view.findViewById(R.id.btnNewSale);
        LinearLayout actionHistory = view.findViewById(R.id.actionHistory);
        LinearLayout actionInventory = view.findViewById(R.id.actionInventory);

        btnNewSale.setOnClickListener(v -> navigateToSales());
        actionHistory.setOnClickListener(v -> navigateToSales()); // Temporarily using Sales for history if needed or just placeholder
        actionInventory.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new InventoryFragment())
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
