package com.example.jijipos.fragments;

import android.graphics.Color;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jijipos.R;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class StaffFragment extends Fragment {

    private TextView textManagerTokenCode;
    private RecyclerView recyclerViewStaff;
    private StaffAdapter staffAdapter;
    private List<User> cashierList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_staff, container, false);

        textManagerTokenCode = view.findViewById(R.id.textManagerTokenCode);
        recyclerViewStaff = view.findViewById(R.id.recyclerViewStaff);
        recyclerViewStaff.setLayoutManager(new LinearLayoutManager(getContext()));

        // Display Manager's registration token code (their phone number from the intent session)
        if (getActivity() != null && getActivity().getIntent() != null) {
            String managerPhone = getActivity().getIntent().getStringExtra("USER_PHONE");
            if (managerPhone != null) {
                textManagerTokenCode.setText(managerPhone);
            }
        }

        loadLinkedCashiersList();
        return view;
    }

    private void loadLinkedCashiersList() {
        if (getActivity() == null || getActivity().getIntent() == null) return;

        // Extract the logged-in manager's active phone number session token
        final String managerPhone = getActivity().getIntent().getStringExtra("USER_PHONE");
        if (managerPhone == null) return;

        // Offload data extraction entirely to a clean background thread pool lane safely
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            // TRIMMED FIX: Pull ONLY cashiers explicitly linked to this specific manager's token
            final List<User> linkedCashiers = db.userDao().getCashiersForManager(managerPhone);

            if (getActivity() != null) {
                // Bounce back onto the main UI thread to update your display widgets safely
                getActivity().runOnUiThread(() -> {
                    cashierList.clear();
                    if (linkedCashiers != null) {
                        cashierList.addAll(linkedCashiers);
                    }

                    // Refresh the recycler view list adapter layout matching your trimmed data bundle
                    staffAdapter = new StaffAdapter(cashierList);
                    recyclerViewStaff.setAdapter(staffAdapter);
                });
            }
        });
    }


    // A lightweight inner Adapter class to recycle the employee rows cleanly without boilerplates
    private static class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {
        private final List<User> staff;

        public StaffAdapter(List<User> staff) { this.staff = staff; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User emp = staff.get(position);
            holder.t1.setText("👤 " + emp.getFullName());
            holder.t2.setText("Phone: " + emp.getPhoneNumber() + " | Role: " + emp.getRole());
            holder.t1.setTextColor(Color.BLACK);
            holder.t2.setTextColor(Color.GRAY);
        }

        @Override
        public int getItemCount() { return staff.size(); }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                t1 = itemView.findViewById(android.R.id.text1);
                t2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
