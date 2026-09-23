package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Settings screen reachable from the dashboard header menu. Hosts the
 * account shortcut, the change-password flow (verified against the stored
 * Room hash), the app PIN shortcut and logout.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText inputCurrentPassword;
    private TextInputEditText inputNewPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton btnSavePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnSettingsBack = findViewById(R.id.btnSettingsBack);
        TextView textSettingsInitials = findViewById(R.id.textSettingsInitials);
        TextView textSettingsName = findViewById(R.id.textSettingsName);
        TextView textSettingsRole = findViewById(R.id.textSettingsRole);
        TextView textSettingsPinState = findViewById(R.id.textSettingsPinState);
        android.widget.LinearLayout rowSettingsProfile = findViewById(R.id.rowSettingsProfile);
        android.widget.LinearLayout rowSettingsPin = findViewById(R.id.rowSettingsPin);
        TextView btnSettingsLogout = findViewById(R.id.btnSettingsLogout);

        inputCurrentPassword = findViewById(R.id.inputCurrentPassword);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        btnSettingsBack.setOnClickListener(v -> finish());

        String sessionName = SessionManager.getUserName(this);
        String sessionRole = SessionManager.getUserRole(this);
        textSettingsInitials.setText(initialsOf(sessionName));
        textSettingsName.setText(sessionName != null ? sessionName : "User");
        textSettingsRole.setText((sessionRole != null ? sessionRole : "CUSTOMER")
                .toUpperCase(Locale.US) + " ACCOUNT");
        textSettingsPinState.setText(SessionManager.hasPin(this)
                ? "Change the 4-digit quick-access PIN"
                : "No PIN set yet - create one");

        rowSettingsProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        rowSettingsPin.setOnClickListener(v ->
                startActivity(SessionManager.attachSession(this,
                        new Intent(this, PinSetupActivity.class))));

        btnSettingsLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnSavePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String current = textOf(inputCurrentPassword);
        String next = textOf(inputNewPassword);
        String confirm = textOf(inputConfirmPassword);

        inputCurrentPassword.setError(null);
        inputNewPassword.setError(null);
        inputConfirmPassword.setError(null);

        if (TextUtils.isEmpty(current)) {
            inputCurrentPassword.setError("Enter your current password");
            return;
        }
        if (TextUtils.isEmpty(next) || next.length() < 6) {
            inputNewPassword.setError("New password must be at least 6 characters");
            return;
        }
        if (!next.equals(confirm)) {
            inputConfirmPassword.setError("Passwords do not match");
            return;
        }
        if (next.equals(current)) {
            inputNewPassword.setError("New password must differ from the current one");
            return;
        }

        final long userId = SessionManager.getUserId(this);
        btnSavePassword.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            User user = db.userDao().getUserById(userId);

            final String error;
            if (user == null) {
                error = "Account not found on this device";
            } else if (!SecurityUtils.hashPassword(current).equals(user.getPasswordHash())) {
                error = "Current password is incorrect";
            } else {
                db.userDao().updatePassword(userId, SecurityUtils.hashPassword(next));
                error = null;
            }

            runOnUiThread(() -> {
                btnSavePassword.setEnabled(true);
                if (error == null) {
                    inputCurrentPassword.setText("");
                    inputNewPassword.setText("");
                    inputConfirmPassword.setText("");
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    inputCurrentPassword.setError(error);
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        String initials = String.valueOf(parts[0].charAt(0));
        if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
        return initials.toUpperCase(Locale.US);
    }
}
