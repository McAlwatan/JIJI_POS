package com.example.jijipos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView textWelcomeBanner = findViewById(R.id.textWelcomeBanner);
        TextView textRoleTag = findViewById(R.id.textRoleTag);
        TextView textDashboardStatus = findViewById(R.id.textDashboardStatus);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        Intent incomingIntent = getIntent();
        String userName = incomingIntent.getStringExtra("USER_NAME");
        String userRole = incomingIntent.getStringExtra("USER_ROLE");

        if (userName == null) userName = "User";
        if (userRole == null) userRole = "CUSTOMER";

        textWelcomeBanner.setText("Hello, " + userName + "!");
        textRoleTag.setText(" " + userRole.toUpperCase() + " ");

        switch (userRole.toUpperCase()) {
            case "MANAGER":
                textDashboardStatus.setText("Business Overview Panel\n\n• Manage Stores\n• Audit Total Cashier Sales\n• Review Inventory Adjustments");
                break;
            case "CASHIER":
                textDashboardStatus.setText("Point of Sale Workspace\n\n• Initialize Product Checkout\n• Generate Receipt QR Codes\n• Enforced App Inventory Sales Only");
                break;
            case "CUSTOMER":
                textDashboardStatus.setText("Customer Personal Space\n\n• View Digital Cash Receipts\n• Track Spending Analytics Category");
                break;
            default:
                textDashboardStatus.setText("System Workspace Unconfigured.");
                break;
        }

        buttonLogout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
