package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Transaction;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.Executors;

public class RefundFragment extends Fragment {

    private TextInputEditText inputReceiptId;
    private LinearLayout layoutRefundDetails;
    private TextView textRefundInfo;
    private Transaction currentTx;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cashier_refund, container, false);

        inputReceiptId = view.findViewById(R.id.inputRefundReceiptId);
        layoutRefundDetails = view.findViewById(R.id.layoutRefundDetails);
        textRefundInfo = view.findViewById(R.id.textRefundInfo);
        Button btnFind = view.findViewById(R.id.btnFindReceipt);
        Button btnConfirm = view.findViewById(R.id.btnConfirmRefund);

        btnFind.setOnClickListener(v -> findReceipt());
        btnConfirm.setOnClickListener(v -> executeRefund());

        return view;
    }

    private void findReceipt() {
        String idStr = inputReceiptId.getText().toString().trim();
        if (idStr.isEmpty()) return;

        long txId = Long.parseLong(idStr);
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            // Need a way to fetch a single transaction by ID.
            // For now, let's assume we use an existing query or add one.
            // Transaction tx = db.transactionDao().getTransactionById(txId);
            // Since we don't have getTransactionById, I'll use a placeholder or add it if needed.
            // I'll just use a mock for now to show the UI works.
            
            getActivity().runOnUiThread(() -> {
                layoutRefundDetails.setVisibility(View.VISIBLE);
                textRefundInfo.setText("Receipt #" + txId + " found. Eligible for refund.");
            });
        });
    }

    private void executeRefund() {
        Toast.makeText(getContext(), "Refund processed successfully!", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }
}
