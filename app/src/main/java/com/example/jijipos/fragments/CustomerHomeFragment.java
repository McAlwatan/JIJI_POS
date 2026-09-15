package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.jijipos.LineGraphView;
import com.example.jijipos.R;
import com.example.jijipos.database.AppDatabase;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CustomerHomeFragment extends Fragment {

    private TextView chipToday, chipWeek, chipMonth, chipYear;
    private TextView textSpendAmount, textSpendLabel;

    private MaterialCardView cardGraphContainer;
    private LineGraphView customerSpendingGraph;
    private boolean isGraphPanelVisible = false;
    private int currentSelectedPeriodIndex = 0; // Tracks active index (0=Today, 1=Week, 2=Month, 3=Year)

    private static final String[] PERIOD_LABELS = {
            "Spent today", "Spent this week", "Spent this month", "Spent this year"
    };

    private static final float[][] SHIFT_TREND_MATRICES = {
            {300000f, 450000f, 1200000f, 850000f, 500000f, 950000f, 1400000f},
            {400000f, 850000f, 600000f, 1300000f, 950000f, 450000f, 1600000f},
            {800000f, 1200000f, 950000f, 1500000f, 700000f, 1100000f, 1950000f},
            {200000f, 600000f, 1400000f, 900000f, 1100000f, 1500000f, 1850000f}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        chipToday = view.findViewById(R.id.chipToday);
        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipYear = view.findViewById(R.id.chipYear);
        textSpendAmount = view.findViewById(R.id.textSpendAmount);
        textSpendLabel = view.findViewById(R.id.textSpendLabel);

        cardGraphContainer = view.findViewById(R.id.cardGraphContainer);
        customerSpendingGraph = view.findViewById(R.id.customerSpendingGraph);

        chipToday.setOnClickListener(v -> selectPeriod(0));
        chipWeek.setOnClickListener(v -> selectPeriod(1));
        chipMonth.setOnClickListener(v -> selectPeriod(2));
        chipYear.setOnClickListener(v -> selectPeriod(3));

        view.findViewById(R.id.btnScanQr).setOnClickListener(v -> {
            View tab2 = requireActivity().findViewById(R.id.tab2);
            if (tab2 != null) tab2.performClick();
        });

        view.findViewById(R.id.actionMyReceipts).setOnClickListener(v -> {
            View tab3 = requireActivity().findViewById(R.id.tab3);
            if (tab3 != null) tab3.performClick();
        });

        view.findViewById(R.id.actionSpendingGraph).setOnClickListener(v -> {
            isGraphPanelVisible = !isGraphPanelVisible;
            cardGraphContainer.setVisibility(isGraphPanelVisible ? View.VISIBLE : View.GONE);

            View panelBg = view.findViewById(R.id.panelBackground);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) panelBg.getLayoutParams();
            params.topToBottom = isGraphPanelVisible ? R.id.cardGraphContainer : R.id.textSpendLabel;
            panelBg.setLayoutParams(params);
        });

        return view;
    }

    // LIFECYCLE HOOK: Pull live total sums automatically whenever the customer returns to this screen
    @Override
    public void onResume() {
        super.onResume();
        selectPeriod(currentSelectedPeriodIndex);
    }

    private void selectPeriod(int index) {
        currentSelectedPeriodIndex = index;
        TextView[] chips = {chipToday, chipWeek, chipMonth, chipYear};

        for (int i = 0; i < chips.length; i++) {
            boolean selected = (i == index);
            chips[i].setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(getResources().getColor(selected ? R.color.white : R.color.text_muted));
        }

        textSpendLabel.setText(PERIOD_LABELS[index]);

        if (customerSpendingGraph != null) {
            customerSpendingGraph.setData(SHIFT_TREND_MATRICES[index]);
        }

        // Trigger the live execution computation block
        calculateLiveExpenses(index);
    }

    private void calculateLiveExpenses(int periodIndex) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            long endTime = System.currentTimeMillis();
            long startTime = 0L;

            Calendar calendar = Calendar.getInstance();
            switch (periodIndex) {
                case 0: // TODAY: Since 12:00 AM midnight
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
                case 1: // WEEK: Past 7 calendar days lookback window
                    startTime = endTime - (7L * 24 * 60 * 60 * 1000);
                    break;
                case 2: // MONTH: Since the 1st day of the current calendar month
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
                case 3: // YEAR: Since January 1st of the current year
                    calendar.set(Calendar.DAY_OF_YEAR, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
            }

            // Execute sum query against customer session ID 99L
            Double totalExpenses = db.transactionDao().getCustomerExpensesSum(99L, startTime, endTime);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    double displaySum = (totalExpenses != null) ? totalExpenses : 0.0;
                    // Format number elegantly with a clean comma separator notation
                    textSpendAmount.setText(String.format(Locale.getDefault(), "TSh %,.0f", displaySum));
                });
            }
        });
    }
}
