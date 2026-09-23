package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;
import com.google.android.material.card.MaterialCardView;

public class ManagerHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);

        MaterialCardView btnReports = view.findViewById(R.id.cardFabCenter);
        LinearLayout actionTeamSales = view.findViewById(R.id.actionTeamSales);
        LinearLayout actionStaff = view.findViewById(R.id.actionStaff);
        LinearLayout actionInventoryControl = view.findViewById(R.id.actionInventoryControl);
        LinearLayout actionReports = view.findViewById(R.id.actionReports);

        actionTeamSales.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new TeamSalesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        actionStaff.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new StaffFragment())
                    .addToBackStack(null)
                    .commit();
        });

        actionInventoryControl.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new InventoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        actionReports.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ManagerReportsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnReports.setOnClickListener(v -> actionReports.performClick());

        // Modern tactile motion: hero FAB springs in, tiles react to press
        com.example.jijipos.UiAnim.popIn(btnReports);
        com.example.jijipos.UiAnim.addPressScale(actionTeamSales);
        com.example.jijipos.UiAnim.addPressScale(actionStaff);
        com.example.jijipos.UiAnim.addPressScale(actionInventoryControl);
        com.example.jijipos.UiAnim.addPressScale(actionReports);

        return view;
    }
}
