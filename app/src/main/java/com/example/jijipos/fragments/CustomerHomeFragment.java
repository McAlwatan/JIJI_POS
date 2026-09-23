package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.example.jijipos.fragments.CustomerReceiptsFragment;
import com.example.jijipos.fragments.PlaceholderFragment;
import com.example.jijipos.fragments.SpendingGraphFragment;
import com.example.jijipos.fragments.CategoriesFragment;

public class CustomerHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        view.findViewById(R.id.actionMyReceipts).setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CustomerReceiptsFragment())
                    .addToBackStack(null)
                    .commit()
        );

        view.findViewById(R.id.actionSpendingGraph).setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SpendingGraphFragment())
                    .addToBackStack(null)
                    .commit()
        );

        view.findViewById(R.id.actionCategoriesBudget).setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CategoriesFragment())
                    .addToBackStack(null)
                    .commit()
        );

        view.findViewById(R.id.actionSplitBill).setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SplitBillFragment())
                    .addToBackStack(null)
                    .commit()
        );

        view.findViewById(R.id.cardQrFloating).setOnClickListener(v -> {
            View tab2 = requireActivity().findViewById(R.id.tab2);
            if (tab2 != null) tab2.performClick();
        });

        return view;
    }
}
