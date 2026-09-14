package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;

public class ManagerHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);

        ImageButton btnReports = view.findViewById(R.id.btnReports);
        LinearLayout actionStaff = view.findViewById(R.id.actionStaff);
        LinearLayout actionInventoryControl = view.findViewById(R.id.actionInventoryControl);

        btnReports.setOnClickListener(v -> {
            // Placeholder for Reports
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

        return view;
    }
}
