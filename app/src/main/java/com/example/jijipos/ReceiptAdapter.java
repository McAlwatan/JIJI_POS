package com.example.jijipos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.jijipos.database.entity.Transaction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ViewHolder> {

    public interface OnReceiptClickListener {
        void onReceiptClick(Transaction transaction);
    }

    private final List<Transaction> transactionList;
    private final OnReceiptClickListener listener;

    public ReceiptAdapter(List<Transaction> transactionList, OnReceiptClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        String displayTitle = transaction.getCategory();
        if (displayTitle == null || displayTitle.trim().isEmpty() || displayTitle.equalsIgnoreCase("Shopping")) {
            displayTitle = "Scanned Digital Invoice";
        }

        holder.textTitle.setText(displayTitle);
        holder.textAmount.setText("TZS " + String.format(Locale.getDefault(), "%,.2f", transaction.getTotalAmount()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.textDate.setText(sdf.format(new Date(transaction.getTimestamp())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onReceiptClick(transaction);
        });
    }


    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDate, textAmount;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textHistoryTitle);
            textDate = itemView.findViewById(R.id.textHistoryDate);
            textAmount = itemView.findViewById(R.id.textHistoryAmount);
        }
    }
}
