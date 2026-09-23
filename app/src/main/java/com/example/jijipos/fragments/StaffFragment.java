package com.example.jijipos.fragments;

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
import com.example.jijipos.database.dao.TransactionDao;
import com.example.jijipos.database.entity.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class StaffFragment extends Fragment {

    private TextView textManagerTokenCode;
    private RecyclerView recyclerViewStaff;
    private StaffAdapter staffAdapter;
    private final List<User> cashierList = new ArrayList<>();
    private final Map<Long, Double> todayTotals = new HashMap<>();

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

            // Pre-compute each cashier's sales total since local midnight for the row badges
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long todayStart = calendar.getTimeInMillis();
            long now = System.currentTimeMillis();

            final Map<Long, Double> computedTotals = new HashMap<>();
            if (linkedCashiers != null) {
                for (User cashier : linkedCashiers) {
                    TransactionDao.SalesStats stats = db.transactionDao()
                            .getCashierSalesStats(cashier.getId(), todayStart, now);
                    computedTotals.put(cashier.getId(), (stats != null && stats.totalSales != null) ? stats.totalSales : 0.0);
                }
            }

            if (getActivity() != null) {
                // Bounce back onto the main UI thread to update your display widgets safely
                getActivity().runOnUiThread(() -> {
                    cashierList.clear();
                    if (linkedCashiers != null) {
                        cashierList.addAll(linkedCashiers);
                    }
                    todayTotals.clear();
                    todayTotals.putAll(computedTotals);

                    // Refresh the recycler view list adapter layout matching your trimmed data bundle
                    if (staffAdapter == null) {
                        staffAdapter = new StaffAdapter(cashierList);
                        recyclerViewStaff.setAdapter(staffAdapter);
                    } else {
                        staffAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }


    // A lightweight inner Adapter class to recycle the employee rows cleanly without boilerplates
    private class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {
        private final List<User> staff;

        public StaffAdapter(List<User> staff) { this.staff = staff; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User emp = staff.get(position);
            holder.textStaffName.setText(emp.getFullName());
            holder.textStaffPhone.setText("Phone: " + emp.getPhoneNumber());
            Double total = todayTotals.get(emp.getId());
            holder.textStaffTodayTotal.setText("TSh " + String.format(java.util.Locale.US, "%,.0f", (total != null) ? total : 0.0));

            holder.itemView.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, CashierReportFragment.newInstance(emp.getId()))
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() { return staff.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textStaffName, textStaffPhone, textStaffTodayTotal;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textStaffName = itemView.findViewById(R.id.textStaffName);
                textStaffPhone = itemView.findViewById(R.id.textStaffPhone);
                textStaffTodayTotal = itemView.findViewById(R.id.textStaffTodayTotal);
            }
        }
    }
}
