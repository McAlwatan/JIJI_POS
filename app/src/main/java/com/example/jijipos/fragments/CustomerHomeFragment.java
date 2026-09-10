package com.example.jijipos.fragments;

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
import com.google.android.material.card.MaterialCardView;

public class CustomerHomeFragment extends Fragment {

    private TextView chipToday, chipWeek, chipMonth, chipYear;
    private TextView textSpendAmount, textSpendLabel;

    // Graph components tracking layout pointers
    private MaterialCardView cardGraphContainer;
    private LineGraphView customerSpendingGraph;
    private boolean isGraphPanelVisible = false;

    private static final String[][] PLACEHOLDER_TOTALS = {
            {"TSh 12,500", "Spent today"},
            {"TSh 84,300", "Spent this week"},
            {"TSh 340,900", "Spent this month"},
            {"TSh 2,150,000", "Spent this year"}
    };

    // Array data clusters mapping custom trend waves matching each lookup window interval
    private static final float[][] SHIFT_TREND_MATRICES = {
            {300000f, 450000f, 1200000f, 850000f, 500000f, 950000f, 1400000f}, // Today Spline Data
            {400000f, 850000f, 600000f, 1300000f, 950000f, 450000f, 1600000f}, // Week Spline Data
            {800000f, 1200000f, 950000f, 1500000f, 700000f, 1100000f, 1950000f}, // Month Spline Data
            {200000f, 600000f, 1400000f, 900000f, 1100000f, 1500000f, 1850000f}  // Year Spline Data
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        // Bind core form layout references
        chipToday = view.findViewById(R.id.chipToday);
        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipYear = view.findViewById(R.id.chipYear);
        textSpendAmount = view.findViewById(R.id.textSpendAmount);
        textSpendLabel = view.findViewById(R.id.textSpendLabel);

        // Bind interactive graphing components
        cardGraphContainer = view.findViewById(R.id.cardGraphContainer);
        customerSpendingGraph = view.findViewById(R.id.customerSpendingGraph);

        // Map button clicks handling period selector changes
        chipToday.setOnClickListener(v -> selectPeriod(0));
        chipWeek.setOnClickListener(v -> selectPeriod(1));
        chipMonth.setOnClickListener(v -> selectPeriod(2));
        chipYear.setOnClickListener(v -> selectPeriod(3));

        // Floating action transaction camera scanner route shortcut
        view.findViewById(R.id.btnScanQr).setOnClickListener(v -> {
            View tab2 = requireActivity().findViewById(R.id.tab2);
            if (tab2 != null) tab2.performClick();
        });

        // Open my receipts recyclerview list history timeline route shortcut
        view.findViewById(R.id.actionMyReceipts).setOnClickListener(v -> {
            View tab3 = requireActivity().findViewById(R.id.tab3);
            if (tab3 != null) tab3.performClick();
        });

        // Dynamically toggle the visibility of the Selcom style chart container on tile tap
        view.findViewById(R.id.actionSpendingGraph).setOnClickListener(v -> {
            isGraphPanelVisible = !isGraphPanelVisible;
            cardGraphContainer.setVisibility(isGraphPanelVisible ? View.VISIBLE : View.GONE);

            // Re-adjust baseline anchoring elements layouts parameters dynamically to prevent overlapping
            View panelBg = view.findViewById(R.id.panelBackground);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) panelBg.getLayoutParams();
            params.topToBottom = isGraphPanelVisible ? R.id.cardGraphContainer : R.id.textSpendLabel;
            panelBg.setLayoutParams(params);
        });

        selectPeriod(0); // Run baseline initialization focus
        return view;
    }

    private void selectPeriod(int index) {
        TextView[] chips = {chipToday, chipWeek, chipMonth, chipYear};
        for (int i = 0; i < chips.length; i++) {
            boolean selected = (i == index);
            chips[i].setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chips[i].setTextColor(getResources().getColor(selected ? R.color.white : R.color.text_muted));
        }

        textSpendAmount.setText(PLACEHOLDER_TOTALS[index][0]);
        textSpendLabel.setText(PLACEHOLDER_TOTALS[index][1]);

        // Push matching period arrays data directly into the View canvas custom drawing paint engine layer
        if (customerSpendingGraph != null) {
            customerSpendingGraph.setData(SHIFT_TREND_MATRICES[index]);
        }
    }
}