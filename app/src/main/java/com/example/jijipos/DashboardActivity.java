package com.example.jijipos;

import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.fragments.CashierHomeFragment;
import com.example.jijipos.fragments.CashierReportFragment;
import com.example.jijipos.fragments.CustomerHomeFragment;
import com.example.jijipos.fragments.CustomerReceiptsFragment;
import com.example.jijipos.fragments.CustomerScanFragment;
import com.example.jijipos.fragments.ManagerHomeFragment;
import com.example.jijipos.fragments.SalesFragment;
import com.example.jijipos.fragments.StaffFragment;
import com.example.jijipos.fragments.CashierTransactionsFragment;
import com.example.jijipos.fragments.InventoryFragment;
import com.example.jijipos.fragments.ManagerReportsFragment;
import com.example.jijipos.fragments.PlaceholderFragment;

import java.util.Locale;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private String userRole;
    private LinearLayout[] tabs;
    private ImageView[] icons;
    private TextView[] texts;

    // Live header balance state so any fragment can trigger a real-time refresh
    private TextView textLiveBalanceValue;
    private ProgressBar balanceProgress;
    private long sessionUserId;
    private long sessionBusinessId;
    private double currentDisplayedBalance = 0.0;
    private ValueAnimator balanceAnimator;

    // Pull-to-refresh state: re-fetch the whole dashboard without logging out
    private SwipeRefreshLayout dashboardSwipeRefresh;
    private int currentTabIndex = 0;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Bind Header Metrics and Informational Text Objects
        TextView textWelcomeBanner = findViewById(R.id.textWelcomeBanner);
        textLiveBalanceValue = findViewById(R.id.textLiveBalanceValue);
        balanceProgress = findViewById(R.id.balanceProgress);

        // Pull-to-refresh: re-reads the header balance and reloads the active screen
        dashboardSwipeRefresh = findViewById(R.id.dashboardSwipeRefresh);
        dashboardSwipeRefresh.setColorSchemeColors(getColor(R.color.teal_light));
        dashboardSwipeRefresh.setProgressBackgroundColorSchemeColor(getColor(R.color.card_dark));
        dashboardSwipeRefresh.setOnRefreshListener(() -> refreshHeaderBalance(false, () -> {
            reloadCurrentScreenIfSafe();
            if (dashboardSwipeRefresh != null) dashboardSwipeRefresh.setRefreshing(false);
        }));

        ImageView btnHeaderMenu = findViewById(R.id.btnHeaderMenu); // Ensure your XML has android:id="@+id/btnHeaderMenu" on the menu icon
        btnHeaderMenu.setOnClickListener(this::showHeaderMenu);

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
        // Unpack dynamic secure session identifiers passed down from login activity
        Intent incomingIntent = getIntent();
        long activeSessionUserId = incomingIntent.getLongExtra("USER_ID", SessionManager.getUserId(this));
        long activeSessionBusinessId = incomingIntent.getLongExtra("BUSINESS_ID", SessionManager.getBusinessId(this));
        this.sessionUserId = activeSessionUserId;
        this.sessionBusinessId = activeSessionBusinessId;
        String userName = incomingIntent.getStringExtra("USER_NAME");
        userRole = incomingIntent.getStringExtra("USER_ROLE");
        String userPhone = incomingIntent.getStringExtra("USER_PHONE");

        // Fall back to the persisted session so a launcher that forgot the
        // extras can never silently downgrade a manager/cashier to customer UI
        if (userName == null) userName = SessionManager.getUserName(this);
        if (userRole == null) userRole = SessionManager.getUserRole(this);
        if (userPhone == null) userPhone = SessionManager.getUserPhone(this);
        if (userName == null) userName = "User";
        if (userRole == null) userRole = "CUSTOMER";

        // Re-stamp the resolved session onto this intent so fragments
        // (e.g. the manager Staff list reading USER_PHONE) always see it
        if (!incomingIntent.hasExtra("USER_ID")) incomingIntent.putExtra("USER_ID", activeSessionUserId);
        if (!incomingIntent.hasExtra("BUSINESS_ID")) incomingIntent.putExtra("BUSINESS_ID", activeSessionBusinessId);
        if (!incomingIntent.hasExtra("USER_NAME")) incomingIntent.putExtra("USER_NAME", userName);
        if (!incomingIntent.hasExtra("USER_ROLE")) incomingIntent.putExtra("USER_ROLE", userRole);
        if (!incomingIntent.hasExtra("USER_PHONE")) incomingIntent.putExtra("USER_PHONE", userPhone);

        textWelcomeBanner.setText(userRole.equalsIgnoreCase("CUSTOMER") ? "Money" : "Habari, " + userName + "!");

        // Visible role badge so the signed-in account type is never ambiguous
        TextView textRoleBadge = findViewById(R.id.textRoleBadge);
        textRoleBadge.setText(userRole.toUpperCase(Locale.US) + " ACCOUNT");

        // 3. Bind Header Toggles and Subtitles (Fairvest Style)
        LinearLayout layoutDashboardToggles = findViewById(R.id.layoutDashboardToggles);
        TextView toggleWallet = findViewById(R.id.toggleWallet);
        TextView toggleAccount = findViewById(R.id.toggleAccount);
        TextView textBalanceSubtitle = findViewById(R.id.textBalanceSubtitle);

        if (userRole.equalsIgnoreCase("CUSTOMER")) {
            layoutDashboardToggles.setVisibility(View.VISIBLE);
            textBalanceSubtitle.setText("Wallet balance");
            
            toggleWallet.setOnClickListener(v -> {
                toggleWallet.setBackgroundResource(R.drawable.bg_nav_active_pill);
                toggleWallet.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_light)));
                toggleWallet.setTextColor(Color.WHITE);
                toggleAccount.setBackground(null);
                toggleAccount.setTextColor(getResources().getColor(R.color.fv_muted));
                textBalanceSubtitle.setText("Wallet balance");
            });

            toggleAccount.setOnClickListener(v -> {
                toggleAccount.setBackgroundResource(R.drawable.bg_nav_active_pill);
                toggleAccount.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_light)));
                toggleAccount.setTextColor(Color.WHITE);
                toggleWallet.setBackground(null);
                toggleWallet.setTextColor(getResources().getColor(R.color.fv_muted));
                textBalanceSubtitle.setText("Account balance");
            });
        } else {
            layoutDashboardToggles.setVisibility(View.GONE);
            if (textBalanceSubtitle != null) {
                textBalanceSubtitle.setText("Active Shift Sales Volume");
            }
        }

        // Kick off the first live header balance load (animated count-up from zero)
        refreshHeaderBalance(true);
        // 4. ROLE-BASED ACCESS CONTROL SEGREGATION MATRIX
        if (userRole.equalsIgnoreCase("CUSTOMER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("Scan");
            icon2.setImageResource(R.drawable.ic_nav_scan);

            text3.setText("Safe");
            icon3.setImageResource(R.drawable.ic_nav_safe);

            text4.setText("More");
            icon4.setImageResource(R.drawable.ic_nav_apps);
            tab4.setVisibility(View.VISIBLE);

            tab1.setOnClickListener(v -> { 
                loadFragment(new CustomerHomeFragment()); 
                updateNavbarState(0); 
                textWelcomeBanner.setText("Dashboard");
            });
            tab2.setOnClickListener(v -> { 
                loadFragment(new CustomerScanFragment()); 
                updateNavbarState(1); 
                textWelcomeBanner.setText("Scan QR");
            });
            tab3.setOnClickListener(v -> { 
                loadFragment(new CustomerReceiptsFragment());
                updateNavbarState(2); 
                textWelcomeBanner.setText("My Receipts");
            });
            tab4.setOnClickListener(v -> { 
                startActivity(new Intent(this, SettingsActivity.class)); 
            });

            loadFragment(new CustomerHomeFragment());
            updateNavbarState(0);
            textWelcomeBanner.setText("Dashboard");

        } else if (userRole.equalsIgnoreCase("CASHIER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("New Sale");
            icon2.setImageResource(R.drawable.ic_nav_sales);

            text3.setText("Reports");
            icon3.setImageResource(R.drawable.ic_chart);

            tab4.setVisibility(View.GONE);

            tab1.setOnClickListener(v -> { loadFragment(new CashierHomeFragment()); updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new SalesFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { loadFragment(CashierReportFragment.newInstance(activeSessionUserId)); updateNavbarState(2); });

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
            tab4.setOnClickListener(v -> { 
                loadFragment(new ManagerReportsFragment()); 
                updateNavbarState(3); 
                textWelcomeBanner.setText("Reports");
            });

            loadFragment(new ManagerHomeFragment());
            updateNavbarState(0);
        }
    }

    // Displays the sleek upper context popup options menu tray block
    private void showHeaderMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 3, 0, "Profile");
        popup.getMenu().add(0, 1, 1, "Settings");
        popup.getMenu().add(0, 2, 2, "Log out");
        popup.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == 2) {
                logout();
                return true;
            }
            if (item.getItemId() == 3) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            if (item.getItemId() == 1) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
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
        currentTabIndex = activeIndex;
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

    /**
     * Recomputes the header balance from Room and updates it live. Runs on
     * first load and again whenever a fragment records a sale or scan, so the
     * figure refreshes in real time without leaving and re-entering the screen.
     * Shows a loading bar while querying, then animates the number to its new
     * value with a smooth count-up.
     */
    public void refreshHeaderBalance(boolean animate) {
        refreshHeaderBalance(animate, null);
    }

    public void refreshHeaderBalance(boolean animate, Runnable onComplete) {
        if (textLiveBalanceValue == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        if (balanceProgress != null) balanceProgress.setVisibility(View.VISIBLE);
        textLiveBalanceValue.animate().alpha(0.5f).setDuration(120).start();

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            long endTime = System.currentTimeMillis();
            long startTime = endTime - (24 * 60 * 60 * 1000); // 24-hour shift timeframe

            final Double computedSalesVolume;
            if (userRole != null && userRole.equalsIgnoreCase("MANAGER")) {
                // MANAGER: enterprise aggregates spanning all store cashiers
                computedSalesVolume = db.transactionDao().getManagerEnterpriseSalesTotal(sessionBusinessId, startTime, endTime);
            } else if (userRole != null && userRole.equalsIgnoreCase("CASHIER")) {
                // CASHIER: their own shift sales entries only
                computedSalesVolume = db.transactionDao().getPersonalCashierSalesTotal(sessionUserId, startTime, endTime);
            } else {
                // CUSTOMER: scanned receipts are attributed to the local customer ledger id
                computedSalesVolume = db.transactionDao().getCustomerExpensesSum(99L, startTime, endTime);
            }

            final double value = (computedSalesVolume != null) ? computedSalesVolume : 0.0;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (balanceProgress != null) balanceProgress.setVisibility(View.GONE);
                textLiveBalanceValue.animate().alpha(1f).setDuration(200).start();
                setBalanceValue(value, animate);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    // Paints the balance, optionally counting up from the previously shown value
    private void setBalanceValue(double newValue, boolean animate) {
        if (textLiveBalanceValue == null) return;
        if (!animate) {
            currentDisplayedBalance = newValue;
            textLiveBalanceValue.setText(String.format(Locale.US, "%,.2f TZS", newValue));
            return;
        }
        if (balanceAnimator != null) balanceAnimator.cancel();
        balanceAnimator = ValueAnimator.ofFloat((float) currentDisplayedBalance, (float) newValue);
        balanceAnimator.setDuration(750);
        balanceAnimator.setInterpolator(new DecelerateInterpolator());
        balanceAnimator.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            textLiveBalanceValue.setText(String.format(Locale.US, "%,.2f TZS", v));
        });
        balanceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                currentDisplayedBalance = newValue;
            }
        });
        balanceAnimator.start();
    }

    // Handles swapping active modular workspaces fragment layers cleanly into view
    private void loadFragment(Fragment fragment) {
        currentFragment = fragment;
        if (dashboardSwipeRefresh != null) {
            // The receipts screen owns its pull-to-refresh; don't nest two spinners
            dashboardSwipeRefresh.setEnabled(!(fragment instanceof CustomerReceiptsFragment));
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit);
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    /**
     * Reloads the active screen on pull-to-refresh so its data is re-queried.
     * Skips the sale form and the scanner so a manual refresh never wipes typed
     * input or restarts the camera mid-scan.
     */
    private void reloadCurrentScreenIfSafe() {
        if (currentFragment instanceof SalesFragment
                || currentFragment instanceof CustomerScanFragment) {
            return;
        }
        if (tabs != null && currentTabIndex >= 0 && currentTabIndex < tabs.length
                && tabs[currentTabIndex] != null) {
            tabs[currentTabIndex].performClick();
        }
    }
}
