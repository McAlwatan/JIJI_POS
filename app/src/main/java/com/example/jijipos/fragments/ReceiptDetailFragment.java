package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;
import com.example.jijipos.database.entity.Transaction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptDetailFragment extends Fragment {

    private Transaction transaction;

    public ReceiptDetailFragment(Transaction transaction) {
        this.transaction = transaction;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipt_detail, container, false);

        ImageButton btnBack = view.findViewById(R.id.btnBackDetail);
        TextView detailCategory = view.findViewById(R.id.detailCategory);
        TextView detailAmount = view.findViewById(R.id.detailAmount);
        
        View rowTime = view.findViewById(R.id.rowTime);
        View rowLocation = view.findViewById(R.id.rowLocation);
        View rowTransactionId = view.findViewById(R.id.rowTransactionId);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (transaction != null) {
            detailCategory.setText(transaction.getCategory());
            detailAmount.setText(String.format(Locale.getDefault(), "TZS %,.2f", transaction.getTotalAmount()));

            setupRow(rowTime, "Time", new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(transaction.getTimestamp())));
            setupRow(rowLocation, "Location", "Dar es Salaam, TZ"); // Mock location for now
            setupRow(rowTransactionId, "Transaction ID", "#" + transaction.getId());
        }

        return view;
    }

    private void setupRow(View row, String label, String value) {
        TextView txtLabel = row.findViewById(R.id.label);
        TextView txtValue = row.findViewById(R.id.value);
        txtLabel.setText(label);
        txtValue.setText(value);
    }
}
