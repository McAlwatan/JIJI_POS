package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Business;
import com.example.jijipos.database.entity.User;

import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Account profile screen reachable from the dashboard header menu for all
 * roles. Shows the signed-in user's identity, role, linked business and
 * security state, read from the live Room record (session prefs first for
 * an instant paint, then refined from the database).
 */
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageButton btnProfileBack = findViewById(R.id.btnProfileBack);
        TextView textProfileInitials = findViewById(R.id.textProfileInitials);
        TextView textProfileName = findViewById(R.id.textProfileName);
        TextView textProfilePhoneHeader = findViewById(R.id.textProfilePhoneHeader);
        TextView textProfileRoleChip = findViewById(R.id.textProfileRoleChip);
        TextView textProfileRoleValue = findViewById(R.id.textProfileRoleValue);
        TextView textProfileBusinessValue = findViewById(R.id.textProfileBusinessValue);
        TextView textProfilePhoneValue = findViewById(R.id.textProfilePhoneValue);
        TextView textProfileIdValue = findViewById(R.id.textProfileIdValue);
        TextView textProfileSecurityValue = findViewById(R.id.textProfileSecurityValue);
        TextView btnProfileLogout = findViewById(R.id.btnProfileLogout);

        btnProfileBack.setOnClickListener(v -> finish());

        final long userId = SessionManager.getUserId(this);
        String sessionName = SessionManager.getUserName(this);
        String sessionRole = SessionManager.getUserRole(this);
        String sessionPhone = SessionManager.getUserPhone(this);

        // Instant paint from the cached session while Room loads
        textProfileInitials.setText(initialsOf(sessionName));
        textProfileName.setText(sessionName != null ? sessionName : "User");
        textProfilePhoneHeader.setText(sessionPhone != null ? sessionPhone : "--");
        textProfileRoleChip.setText(sessionRole != null ? sessionRole : "CUSTOMER");
        textProfileRoleValue.setText(capitalize(sessionRole));
        textProfilePhoneValue.setText(sessionPhone != null ? sessionPhone : "--");
        textProfileIdValue.setText("#" + userId);
        textProfileSecurityValue.setText(SessionManager.hasPin(this) ? "PIN protected" : "No PIN set");
        textProfileBusinessValue.setText("Loading...");

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            User user = db.userDao().getUserById(userId);

            String businessName = null;
            if (user != null && user.getBusinessId() != null && user.getBusinessId() > 0) {
                Business business = db.businessDao().getBusinessById(user.getBusinessId());
                if (business != null) businessName = business.getBusinessName();
            }

            final User finalUser = user;
            final String finalBusiness = businessName;
            runOnUiThread(() -> {
                if (finalUser != null) {
                    textProfileInitials.setText(initialsOf(finalUser.getFullName()));
                    textProfileName.setText(finalUser.getFullName());
                    textProfilePhoneHeader.setText(finalUser.getPhoneNumber());
                    textProfileRoleChip.setText(finalUser.getRole());
                    textProfileRoleValue.setText(capitalize(finalUser.getRole()));
                    textProfilePhoneValue.setText(finalUser.getPhoneNumber());
                    textProfileIdValue.setText("#" + finalUser.getId());
                }
                textProfileBusinessValue.setText(finalBusiness != null ? finalBusiness : "Independent account");
            });
        });

        btnProfileLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        String initials = String.valueOf(parts[0].charAt(0));
        if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
        return initials.toUpperCase(Locale.US);
    }

    private String capitalize(String role) {
        if (role == null || role.isEmpty()) return "Customer";
        return role.substring(0, 1).toUpperCase(Locale.US) + role.substring(1).toLowerCase(Locale.US);
    }
}
