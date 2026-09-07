package com.example.jijipos;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.jijipos.fragments.CustomerHomeFragment;
import com.example.jijipos.fragments.CustomerScanFragment;
import com.example.jijipos.fragments.SalesFragment;

public class DashboardActivity extends AppCompatActivity {

    private String userRole;
    private LinearLayout[] tabs;
    private ImageView[] icons;
    private TextView[] texts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView textWelcomeBanner = findViewById(R.id.textWelcomeBanner);
        TextView textRoleTag = findViewById(R.id.textRoleTag);
        Button buttonLogout = findViewById(R.id.buttonLogout);

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

        View divider1 = findViewById(R.id.divider1);
        View divider2 = findViewById(R.id.divider2);
        View divider3 = findViewById(R.id.divider3);

        tabs = new LinearLayout[]{tab1, tab2, tab3, tab4};
        icons = new ImageView[]{icon1, icon2, icon3, icon4};
        texts = new TextView[]{text1, text2, text3, text4};

        Intent incomingIntent = getIntent();
        String userName = incomingIntent.getStringExtra("USER_NAME");
        userRole = incomingIntent.getStringExtra("USER_ROLE");

        if (userName == null) userName = "User";
        if (userRole == null) userRole = "CUSTOMER";

        textWelcomeBanner.setText("Habari, " + userName + "!");
        textRoleTag.setText(" " + userRole.toUpperCase().trim() + " ");

        if (userRole.equalsIgnoreCase("CUSTOMER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("Scan QR");
            icon2.setImageResource(R.drawable.ic_nav_scan);

            text3.setText("History");
            icon3.setImageResource(android.R.drawable.ic_menu_agenda);

            tab4.setVisibility(View.GONE);
            divider3.setVisibility(View.GONE);

            tab1.setOnClickListener(v -> { loadFragment(new CustomerHomeFragment()); updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new CustomerScanFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { /* loadFragment(new CustomerReceiptsFragment()); */ updateNavbarState(2); });

            loadFragment(new CustomerHomeFragment());
            updateNavbarState(0);

        } else if (userRole.equalsIgnoreCase("CASHIER")) {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("New Sale");
            icon2.setImageResource(R.drawable.ic_nav_sales);

            text3.setText("History");
            icon3.setImageResource(android.R.drawable.ic_menu_recent_history);

            tab4.setVisibility(View.GONE);
            divider3.setVisibility(View.GONE);

            tab1.setOnClickListener(v -> { /* loadFragment(new CashierHomeFragment()); */ updateNavbarState(0); });
            tab2.setOnClickListener(v -> { loadFragment(new SalesFragment()); updateNavbarState(1); });
            tab3.setOnClickListener(v -> { /* loadFragment(new CashierTransactionsFragment()); */ updateNavbarState(2); });

            loadFragment(new SalesFragment());
            updateNavbarState(1);

        } else {
            text1.setText("Home");
            icon1.setImageResource(R.drawable.ic_nav_home);

            text2.setText("Overview");
            icon2.setImageResource(R.drawable.ic_nav_sales);

            text3.setText("Staff");
            icon3.setImageResource(R.drawable.ic_nav_staff);

            text4.setText("Refunds");
            icon4.setImageResource(android.R.drawable.ic_menu_revert);

            tab1.setOnClickListener(v -> { updateNavbarState(0); });
            tab2.setOnClickListener(v -> { updateNavbarState(1); });
            tab3.setOnClickListener(v -> { updateNavbarState(2); });
            tab4.setOnClickListener(v -> { updateNavbarState(3); });

            updateNavbarState(0);
        }

        buttonLogout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateNavbarState(int activeIndex) {
        for (int i = 0; i < tabs.length; i++) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabs[i].getLayoutParams();
            if (i == activeIndex) {
                tabs[i].setBackgroundResource(R.drawable.bg_nav_active_pill);
                icons[i].setImageTintList(ColorStateList.valueOf(Color.WHITE));
                texts[i].setVisibility(View.VISIBLE);
                params.weight = 1.3f;
            } else {
                tabs[i].setBackgroundResource(android.R.color.transparent);
                icons[i].setImageTintList(ColorStateList.valueOf(Color.parseColor("#88FFFFFF")));
                texts[i].setVisibility(View.GONE);
                params.weight = 1.0f;
            }
            tabs[i].setLayoutParams(params);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}
