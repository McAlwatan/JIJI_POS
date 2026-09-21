package com.example.jijipos.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.jijipos.LineGraphView;
import com.example.jijipos.R;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.dao.TransactionDao;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    // Fixed customer id used elsewhere in this fragment/app for the demo
    // single-customer flow — unchanged from the original.
    private static final long CUSTOMER_ID = 99L;

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

        // Trigger the live execution computation block (total AND graph,
        // both pulled from the same query window so they can't disagree).
        calculateLiveExpenses(index);
    }

    private void calculateLiveExpenses(int periodIndex) {
        Context context = getContext();
        if (context == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            long endTime = System.currentTimeMillis();
            long startTime;
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
                    // This is also what makes the graph "reset" every month —
                    // startTime recomputes to day 1 every time this runs, so
                    // there's nothing left over from the previous month.
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
                case 3: // YEAR: Since January 1st of the current year
                    calendar.set(Calendar.DAY_OF_YEAR, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
                default:
                    startTime = 0L;
            }

            // Execute sum query against customer session ID
            Double totalExpenses = db.transactionDao().getCustomerExpensesSum(CUSTOMER_ID, startTime, endTime);

            TrendBuckets trend;
            switch (periodIndex) {
                case 0:
                    trend = buildHourlyBuckets(db, startTime, endTime);
                    break;
                case 1:
                    trend = buildDailyBucketsTrailing(db, startTime, endTime, 7);
                    break;
                case 2:
                    trend = buildDailyBucketsForMonth(db, startTime);
                    break;
                case 3:
                default:
                    trend = buildMonthlyBuckets(db, startTime);
                    break;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    double displaySum = (totalExpenses != null) ? totalExpenses : 0.0;
                    textSpendAmount.setText(String.format(Locale.getDefault(), "TSh %,.0f", displaySum));

                    if (customerSpendingGraph != null) {
                        customerSpendingGraph.setData(trend.labels, trend.values);
                    }
                });
            }
        });
    }

    // ===================== Trend-bucket builders =====================
    // Each pulls real rows via TransactionDao's grouped queries, then
    // zero-fills any bucket with no transactions so the axis stays
    // continuous (a quiet hour/day/month is a real 0, not a gap).

    private TrendBuckets buildHourlyBuckets(AppDatabase db, long startTime, long endTime) {
        List<TransactionDao.SpendBucket> rows = db.transactionDao().getCustomerSpendByHour(CUSTOMER_ID, startTime, endTime);
        Map<String, Double> byHour = new HashMap<>();
        for (TransactionDao.SpendBucket row : rows) byHour.put(row.bucketLabel, row.bucketTotal);

        String[] labels = new String[24];
        float[] values = new float[24];
        for (int h = 0; h < 24; h++) {
            String key = String.format(Locale.US, "%02d", h);
            Double sum = byHour.get(key);
            values[h] = (sum != null) ? sum.floatValue() : 0f;
            // Label every 3rd hour only — 24 labels on one axis would overlap.
            labels[h] = (h % 3 == 0) ? formatHourLabel(h) : "";
        }
        return new TrendBuckets(labels, values);
    }

    private String formatHourLabel(int hour24) {
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        String suffix = (hour24 < 12) ? "a" : "p";
        return hour12 + suffix;
    }

    /** Last {@code dayCount} calendar days ending today, oldest first. */
    private TrendBuckets buildDailyBucketsTrailing(AppDatabase db, long startTime, long endTime, int dayCount) {
        List<TransactionDao.SpendBucket> rows = db.transactionDao().getCustomerSpendByDay(CUSTOMER_ID, startTime, endTime);
        Map<String, Double> byDay = new HashMap<>();
        for (TransactionDao.SpendBucket row : rows) byDay.put(row.bucketLabel, row.bucketTotal);

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE", Locale.US); // Mon, Tue, ...

        String[] labels = new String[dayCount];
        float[] values = new float[dayCount];

        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(endTime);
        for (int i = dayCount - 1; i >= 0; i--) {
            String key = keyFmt.format(cursor.getTime());
            Double sum = byDay.get(key);
            values[i] = (sum != null) ? sum.floatValue() : 0f;
            labels[i] = labelFmt.format(cursor.getTime());
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }
        return new TrendBuckets(labels, values);
    }

    /** Day 1 of the current month through today, labeled by day-of-month. */
    private TrendBuckets buildDailyBucketsForMonth(AppDatabase db, long monthStartTime) {
        long endTime = System.currentTimeMillis();
        List<TransactionDao.SpendBucket> rows = db.transactionDao().getCustomerSpendByDay(CUSTOMER_ID, monthStartTime, endTime);
        Map<String, Double> byDay = new HashMap<>();
        for (TransactionDao.SpendBucket row : rows) byDay.put(row.bucketLabel, row.bucketTotal);

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        int daysSoFar = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        String[] labels = new String[daysSoFar];
        float[] values = new float[daysSoFar];

        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(monthStartTime);
        for (int d = 0; d < daysSoFar; d++) {
            String key = keyFmt.format(cursor.getTime());
            Double sum = byDay.get(key);
            values[d] = (sum != null) ? sum.floatValue() : 0f;
            int dayNumber = d + 1;
            // Every 5th day plus day 1 keeps a ~31-point axis readable.
            labels[d] = (dayNumber == 1 || dayNumber % 5 == 0) ? String.valueOf(dayNumber) : "";
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new TrendBuckets(labels, values);
    }

    /** January through the current month, labeled by month abbreviation. */
    private TrendBuckets buildMonthlyBuckets(AppDatabase db, long yearStartTime) {
        long endTime = System.currentTimeMillis();
        List<TransactionDao.SpendBucket> rows = db.transactionDao().getCustomerSpendByMonth(CUSTOMER_ID, yearStartTime, endTime);
        Map<String, Double> byMonth = new HashMap<>();
        for (TransactionDao.SpendBucket row : rows) byMonth.put(row.bucketLabel, row.bucketTotal);

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM", Locale.US);
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM", Locale.US); // Jan, Feb, ...

        int monthsSoFar = Calendar.getInstance().get(Calendar.MONTH) + 1; // 1-12, current month inclusive
        String[] labels = new String[monthsSoFar];
        float[] values = new float[monthsSoFar];

        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(yearStartTime);
        for (int m = 0; m < monthsSoFar; m++) {
            String key = keyFmt.format(cursor.getTime());
            Double sum = byMonth.get(key);
            values[m] = (sum != null) ? sum.floatValue() : 0f;
            labels[m] = labelFmt.format(cursor.getTime());
            cursor.add(Calendar.MONTH, 1);
        }
        return new TrendBuckets(labels, values);
    }

    private static class TrendBuckets {
        final String[] labels;
        final float[] values;

        TrendBuckets(String[] labels, float[] values) {
            this.labels = labels;
            this.values = values;
        }
    }
}