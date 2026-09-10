package com.example.jijipos;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.fragments.CashierHomeFragment;
import com.example.jijipos.fragments.CustomerHomeFragment;
import com.example.jijipos.fragments.CustomerScanFragment;
import com.example.jijipos.fragments.ManagerHomeFragment;
import com.example.jijipos.fragments.SalesFragment;
import com.example.jijipos.fragments.StaffFragment;
import com.example.jijipos.fragments.InventoryFragment;

import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private String userRole;
    private LinearLayout[] tabs;
    private ImageView[] icons;
    private TextView[] texts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Bind Header Metrics and Informational Text Objects
        TextView textWelcomeBanner = findViewById(R.id.textWelcomeBanner);
        TextView textLiveBalanceValue = findViewById(R.id.textLiveBalanceValue);
        ImageView btnHeaderMenu = findViewById(R.id.btnHeaderMenu); // Ensure your XML has android:id="@+id/btnHeaderMenu" on the menu icon

        // 2. Bind the Custom Horizontal Bottom Navigation Bar Layout Containers
        LinearLayout tab1 = findViewById(R.id.tab1);
        LinearLayout tab2 = findViewById(R.id.tab2);
        LinearLayout tab3 = findViewById(R.id.tab3);
        LinearLayout tab4 = findViewById(R.id.tab4);

        ImageView icon1 = findViewById(R.id.icon1);
        ImageView icon2 = findViewById(R.id.icon2);
        ImageView icon3 = findViewById(R.id.icon3);
        ImageView icon4 = findViewById(R.id.icon4);

        TextView text1 = findViewById(R.id.text1);
        TextView text2 = findViewById(R.id.text2);
        TextView text3 = findViewById(R.id.text3);
        TextView text4 = findViewById(R.id.text4);

        // Group structural layout elements into standard processing clusters
        tabs = new LinearLayout[]{tab1, tab2, tab3, tab4};
        icons = new ImageView[]{icon1, icon2, icon3, icon4};
        texts = new TextView[]{text1, text2, text3, text4};

        // 3. Unpack User Account Extras passed down from Login Activity
        Intent incomingIntent = getIntent();
        String userName = incomingIntent.getStringExtra("USER_NAME");
        userRole = incomingIntent.getStringExtra("USER_ROLE");

        if (userName == null) userName = "User";
        if (userRole == null) userRole = "CUSTOMER";

        textWelcomeBanner.setText("Habari, " + userName + "!");

        // Set up click listener for your premium top header overflow menu popup
        if (btnHeaderMenu != null) {
            btnHeaderMenu.setOnClickListener(this::showHeaderMenu);
        }

        // ========================================================
        // LIVE DYNAMIC SHIFT SALES AGGREGATES LEDGER ENGINE
        // ========================================================
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // Calculate time lookup boundaries matching exactly the current 24-hour window
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (24 * 60 * 60 * 1000);

            // FIX: Call it synchronously on the background thread without the lambda callback argument
            Double totalSalesVolume = db.transactionDao().getBusinessSalesTotal(1L, startTime, endTime);

            runOnUiThread(() -> {
                double finalDisplayTotal = (totalSalesVolume != null) ? totalSalesVolume : 0.0;

                // Format output numbers cleanly into your currency template text box layout
                if (textLiveBalanceValue != null) {
                    textLiveBalanceValue.setText(String.format(Locale.US, "%,.2f TZS", finalDisplayTotal));
                }
            });
        });
        ;
        // ========================================================

        // 4. ROLE-BASED ACCESS CONTROL SEGREGATION MATRIX
        if (userRole.equalsIgnoreCase("CUSTOMER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("Scan QR");
            icon2.setImageResource(R.drawable.ic_nav_scan);

            text3.setText("History");
            icon3.setImageResource(android.R.drawable.ic_menu_agenda);

            tab4.setVisibility(View.GONE);

            tab1.setOnClickListener(v -> { loadFragment(new CustomerHomeFragment()); updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new CustomerScanFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { loadFragment(new com.example.jijipos.fragments.CustomerReceiptsFragment()); updateNavbarState(2); });

            loadFragment(new CustomerHomeFragment());
            updateNavbarState(0);

        } else if (userRole.equalsIgnoreCase("CASHIER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("New Sale");
            icon2.setImageResource(R.drawable.ic_nav_sales);

            text3.setText("Inventory");
            icon3.setImageResource(R.drawable.ic_nav_inventory);

            tab4.setVisibility(View.GONE);

            tab1.setOnClickListener(v -> { loadFragment(new CashierHomeFragment()); updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new SalesFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { loadFragment(new InventoryFragment()); updateNavbarState(2); });

            loadFragment(new CashierHomeFragment());
            updateNavbarState(0);

        } else {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("Staff");
            icon2.setImageResource(R.drawable.ic_nav_staff);

            text3.setText("Inventory");
            icon3.setImageResource(R.drawable.ic_nav_inventory);

            text4.setText("Reports");
            icon4.setImageResource(R.drawable.ic_chart);

            tab1.setOnClickListener(v -> { loadFragment(new ManagerHomeFragment()); updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new StaffFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { loadFragment(new InventoryFragment()); updateNavbarState(2); });
            tab4.setOnClickListener(v -> { /* loadFragment(new ReportsFragment()); */ updateNavbarState(3); });

            loadFragment(new ManagerHomeFragment());
            updateNavbarState(0);
        }
    }

    // Displays the sleek upper context popup options menu tray block
    private void showHeaderMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Settings");
        popup.getMenu().add(0, 2, 1, "Logout");
        popup.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == 2) {
                logout();
                return true;
            }
            // TODO: wire Settings screen when it exists configuration frames
            return true;
        });
        popup.show();
    }

    // Handles clearing active user login session caches securely
    private void logout() {
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Premium flat nav configuration update method.
     * Keeps text and icon assets visible at all times, gracefully adjusting opacities.
     */
    private void updateNavbarState(int activeIndex) {
        int colorActiveAccent = Color.WHITE;
        int colorInactiveDim = Color.parseColor("#55FFFFFF"); // Frosted translucent white styling color

        for (int i = 0; i < tabs.length; i++) {
            if (i == activeIndex) {
                icons[i].setImageTintList(ColorStateList.valueOf(colorActiveAccent));
                texts[i].setTextColor(colorActiveAccent);
                texts[i].setAlpha(1.0f);
            } else {
                icons[i].setImageTintList(ColorStateList.valueOf(colorInactiveDim));
                texts[i].setTextColor(colorInactiveDim);
                texts[i].setAlpha(0.6f);
            }
        }
    }

    // Handles swapping active modular workspaces fragment layers cleanly into view
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}
