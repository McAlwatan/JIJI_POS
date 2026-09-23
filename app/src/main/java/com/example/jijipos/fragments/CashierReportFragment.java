package com.example.jijipos.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jijipos.LineGraphView;
import com.example.jijipos.R;
import com.example.jijipos.ReceiptAdapter;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.dao.TransactionDao;
import com.example.jijipos.database.entity.Transaction;
import com.example.jijipos.database.entity.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Dual-purpose sales report screen:
 *  - MANAGER opens it (with a cashier id argument) from the Staff list to
 *    drill into one cashier's performance.
 *  - CASHIER opens it in "self mode" (argument id == session user id) to
 *    generate their own Day / Week / Month / Year sales report.
 * Both modes share the period chips, stats card, trend graph and the
 * share-report action that exports a plain-text summary.
 */
public class CashierReportFragment extends Fragment {

    private static final String ARG_CASHIER_ID = "CASHIER_ID";

    private enum Period {
        DAY("Today"), WEEK("This Week"), MONTH("This Month"), YEAR("This Year");

        final String displayName;

        Period(String displayName) { this.displayName = displayName; }

        /** Start-of-period timestamp (local midnight of day/week/month/year). */
        long startMillis() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (this == WEEK) {
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            } else if (this == MONTH) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
            } else if (this == YEAR) {
                cal.set(Calendar.DAY_OF_YEAR, 1);
            }
            return cal.getTimeInMillis();
        }
    }

    private long cashierId;
    private boolean selfMode;
    private Period activePeriod = Period.DAY;

    private ImageButton btnBack, btnShareReport;
    private TextView textCashierNameHeader, textCashierPhoneSub;
    private TextView textReportPeriodLabel, textCashierSalesTotal, textReportSaleCount, textReportAvgSale;
    private TextView textReportListHeader;
    private TextView chipDay, chipWeek, chipMonth, chipYear;
    private LineGraphView graphReportTrend;
    private RecyclerView recyclerViewCashierTransactions;

    private final List<Transaction> transactionsList = new ArrayList<>();
    private ReceiptAdapter adapter;

    private String cashierName = "Cashier";
    private String cashierPhone = "--";
    private double lastTotal = 0.0;
    private int lastCount = 0;

    public static CashierReportFragment newInstance(long cashierId) {
        CashierReportFragment fragment = new CashierReportFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CASHIER_ID, cashierId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cashierId = getArguments().getLong(ARG_CASHIER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_cashier_report, container, false);

        btnBack = view.findViewById(R.id.btnBack);
        btnShareReport = view.findViewById(R.id.btnShareReport);
        textCashierNameHeader = view.findViewById(R.id.textCashierNameHeader);
        textCashierPhoneSub = view.findViewById(R.id.textCashierPhoneSub);
        textReportPeriodLabel = view.findViewById(R.id.textReportPeriodLabel);
        textCashierSalesTotal = view.findViewById(R.id.textCashierSalesTotal);
        textReportSaleCount = view.findViewById(R.id.textReportSaleCount);
        textReportAvgSale = view.findViewById(R.id.textReportAvgSale);
        textReportListHeader = view.findViewById(R.id.textReportListHeader);
        chipDay = view.findViewById(R.id.chipDay);
        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipYear = view.findViewById(R.id.chipYear);
        graphReportTrend = view.findViewById(R.id.graphReportTrend);
        recyclerViewCashierTransactions = view.findViewById(R.id.recyclerViewCashierTransactions);

        recyclerViewCashierTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReceiptAdapter(transactionsList, tx -> {
            // Receipt rows are informational inside the report view
        });
        recyclerViewCashierTransactions.setAdapter(adapter);

        long sessionUserId = 0L;
        if (getActivity() != null && getActivity().getIntent() != null) {
            sessionUserId = getActivity().getIntent().getLongExtra("USER_ID", 0L);
        }
        selfMode = (sessionUserId == cashierId);
        if (selfMode) {
            btnBack.setVisibility(View.GONE);
            textCashierNameHeader.setText("My Sales Report");
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnShareReport.setOnClickListener(v -> shareCurrentReport());

        chipDay.setOnClickListener(v -> selectPeriod(Period.DAY));
        chipWeek.setOnClickListener(v -> selectPeriod(Period.WEEK));
        chipMonth.setOnClickListener(v -> selectPeriod(Period.MONTH));
        chipYear.setOnClickListener(v -> selectPeriod(Period.YEAR));
        updateChipSelection();

        loadReportData();
        return view;
    }

    private void selectPeriod(Period period) {
        if (activePeriod == period) return;
        activePeriod = period;
        updateChipSelection();
        loadReportData();
    }

    private void updateChipSelection() {
        TextView[] chips = {chipDay, chipWeek, chipMonth, chipYear};
        Period[] periods = Period.values();
        for (int i = 0; i < chips.length; i++) {
            boolean selected = (periods[i] == activePeriod);
            chips[i].setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(getResources().getColor(selected ? R.color.white : R.color.text_muted));
        }
    }

    private void loadReportData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            AppDatabase db = AppDatabase.getInstance(getContext());
            User cashier = db.userDao().getUserById(cashierId);

            long startTime = activePeriod.startMillis();
            long endTime = System.currentTimeMillis();

            TransactionDao.SalesStats stats = db.transactionDao().getCashierSalesStats(cashierId, startTime, endTime);
            List<Transaction> transactions = db.transactionDao().getTransactionsByCashierInRange(cashierId, startTime, endTime);

            List<TransactionDao.SpendBucket> buckets;
            if (activePeriod == Period.DAY) {
                buckets = db.transactionDao().getCashierSpendByHour(cashierId, startTime, endTime);
            } else if (activePeriod == Period.YEAR) {
                buckets = db.transactionDao().getCashierSpendByMonth(cashierId, startTime, endTime);
            } else {
                buckets = db.transactionDao().getCashierSpendByDay(cashierId, startTime, endTime);
            }

            final double total = (stats != null && stats.totalSales != null) ? stats.totalSales : 0.0;
            final int count = (stats != null) ? stats.saleCount : 0;
            final String name = (cashier != null) ? cashier.getFullName() : "Cashier";
            final String phone = (cashier != null) ? cashier.getPhoneNumber() : "--";
            final GraphSeries series = buildGraphSeries(buckets);
            final List<Transaction> safeTransactions = (transactions != null) ? transactions : new ArrayList<Transaction>();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    cashierName = name;
                    cashierPhone = phone;
                    lastTotal = total;
                    lastCount = count;

                    if (!selfMode) {
                        textCashierNameHeader.setText(name);
                    }
                    textCashierPhoneSub.setText("Phone: " + phone);
                    textReportPeriodLabel.setText("TOTAL SALES — " + activePeriod.displayName.toUpperCase(Locale.US));
                    textCashierSalesTotal.setText("TSh " + String.format(Locale.US, "%,.0f", total));
                    textReportSaleCount.setText(String.valueOf(count));
                    double average = (count > 0) ? (total / count) : 0.0;
                    textReportAvgSale.setText("TSh " + String.format(Locale.US, "%,.0f", average));
                    textReportListHeader.setText(activePeriod.displayName + " receipts (" + count + ")");

                    graphReportTrend.setData(series.labels, series.values);

                    transactionsList.clear();
                    transactionsList.addAll(safeTransactions);
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    /**
     * Turns sparse SQL buckets into a continuous zero-filled series so the
     * graph always renders a full day (24h), week (Mon-Sun), month (1-31)
     * or year (Jan-Dec) axis.
     */
    private GraphSeries buildGraphSeries(List<TransactionDao.SpendBucket> buckets) {
        Map<String, Double> bucketMap = new HashMap<>();
        if (buckets != null) {
            for (TransactionDao.SpendBucket b : buckets) {
                bucketMap.put(b.bucketLabel, b.bucketTotal);
            }
        }

        String[] labels;
        float[] values;

        if (activePeriod == Period.DAY) {
            labels = new String[24];
            values = new float[24];
            for (int hour = 0; hour < 24; hour++) {
                String key = String.format(Locale.US, "%02d", hour);
                labels[hour] = (hour % 4 == 0) ? key : "";
                Double v = bucketMap.get(key);
                values[hour] = (v != null) ? v.floatValue() : 0f;
            }
        } else if (activePeriod == Period.WEEK) {
            labels = new String[7];
            values = new float[7];
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat labelFormat = new SimpleDateFormat("EEE", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(activePeriod.startMillis());
            for (int day = 0; day < 7; day++) {
                labels[day] = labelFormat.format(cal.getTime());
                Double v = bucketMap.get(keyFormat.format(cal.getTime()));
                values[day] = (v != null) ? v.floatValue() : 0f;
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        } else if (activePeriod == Period.MONTH) {
            Calendar cal = Calendar.getInstance();
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String monthKey = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
            labels = new String[daysInMonth];
            values = new float[daysInMonth];
            for (int day = 1; day <= daysInMonth; day++) {
                labels[day - 1] = (day % 5 == 1) ? String.valueOf(day) : "";
                Double v = bucketMap.get(monthKey + "-" + String.format(Locale.US, "%02d", day));
                values[day - 1] = (v != null) ? v.floatValue() : 0f;
            }
        } else {
            labels = new String[12];
            values = new float[12];
            String yearKey = new SimpleDateFormat("yyyy", Locale.US).format(new Date());
            SimpleDateFormat monthLabelFormat = new SimpleDateFormat("MMM", Locale.US);
            Calendar cal = Calendar.getInstance();
            for (int month = 1; month <= 12; month++) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.MONTH, month - 1);
                labels[month - 1] = (month % 2 == 1) ? monthLabelFormat.format(cal.getTime()) : "";
                Double v = bucketMap.get(yearKey + "-" + String.format(Locale.US, "%02d", month));
                values[month - 1] = (v != null) ? v.floatValue() : 0f;
            }
        }
        return new GraphSeries(labels, values);
    }

    /** X-axis labels + Y values pair handed to LineGraphView. */
    private static class GraphSeries {
        final String[] labels;
        final float[] values;

        GraphSeries(String[] labels, float[] values) {
            this.labels = labels;
            this.values = values;
        }
    }

    /** Exports the currently displayed period as a shareable plain-text report. */
    private void shareCurrentReport() {
        StringBuilder report = new StringBuilder();
        report.append("JIJI POS — SALES REPORT\n");
        report.append("Cashier: ").append(cashierName).append(" (").append(cashierPhone).append(")\n");
        report.append("Period: ").append(activePeriod.displayName).append("\n");
        report.append("Total: TZS ").append(String.format(Locale.US, "%,.2f", lastTotal)).append("\n");
        report.append("Sales made: ").append(lastCount).append("\n");
        double average = (lastCount > 0) ? (lastTotal / lastCount) : 0.0;
        report.append("Average per sale: TZS ").append(String.format(Locale.US, "%,.2f", average)).append("\n");
        report.append("------------------------------\n");

        if (transactionsList.isEmpty()) {
            report.append("No sales recorded in this period.\n");
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            for (Transaction tx : transactionsList) {
                report.append(formatter.format(new Date(tx.getTimestamp())))
                        .append("  #").append(tx.getId())
                        .append("  ").append(tx.getCategory() != null ? tx.getCategory() : "Sale")
                        .append("  TZS ").append(String.format(Locale.US, "%,.2f", tx.getTotalAmount()))
                        .append("\n");
            }
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "JIJI POS sales report — " + activePeriod.displayName);
        sendIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(sendIntent, "Share sales report"));
    }
}
