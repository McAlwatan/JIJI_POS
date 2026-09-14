package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;
import com.google.android.material.textfield.TextInputEditText;

public class InventoryFragment extends Fragment {

    private TextInputEditText inputStockName, inputStockQty;
    private Button buttonUpdateStock;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // FIX: Inflate the dedicated manager inventory management control panel layout
        View view = inflater.inflate(R.layout.fragment_manager_inventory, container, false);

        inputStockName = view.findViewById(R.id.inputStockName);
        inputStockQty = view.findViewById(R.id.inputStockQty);
        buttonUpdateStock = view.findViewById(R.id.buttonUpdateStock);

        buttonUpdateStock.setOnClickListener(v -> {
            String name = inputStockName.getText().toString().trim();
            String qty = inputStockQty.getText().toString().trim();

            if (!name.isEmpty() && !qty.isEmpty()) {
                Toast.makeText(getContext(), "Product: " + name + " stock increased by " + qty, Toast.LENGTH_SHORT).show();
                inputStockName.setText("");
                inputStockQty.setText("");
            } else {
                Toast.makeText(getContext(), "Please fill in all stock values!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
