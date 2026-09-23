package com.example.jijipos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class PinLockActivity extends AppCompatActivity {

    private View[] dots;
    private StringBuilder pinBuilder = new StringBuilder();
    private String savedPin;
    private String userName, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        SharedPreferences prefs = getSharedPreferences("JIJI_POS_SECURITY_PREFS", Context.MODE_PRIVATE);
        savedPin = prefs.getString("SECURITY_PIN_KEY", null);
        userName = prefs.getString("SAVED_USER_NAME", "User");
        userRole = prefs.getString("SAVED_USER_ROLE", "CUSTOMER");

        if (savedPin == null) {
            // If no PIN is set, force standard login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        dots = new View[]{
                findViewById(R.id.dot1),
                findViewById(R.id.dot2),
                findViewById(R.id.dot3),
                findViewById(R.id.dot4)
        };

        setupNumericGrid();

        findViewById(R.id.btnBiometricUnlock).setOnClickListener(v -> showBiometricPrompt());
        findViewById(R.id.btnDeletePin).setOnClickListener(v -> deleteLastDigit());
        findViewById(R.id.textSwitchToLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Automatically show biometric prompt on start
        showBiometricPrompt();
    }

    private void setupNumericGrid() {
        int[] buttonIds = {
                R.id.pinGrid // We'll iterate through children
        };
        GridLayout grid = findViewById(R.id.pinGrid);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof Button) {
                Button b = (Button) child;
                if (b.getTag() != null) {
                    b.setOnClickListener(v -> addDigit(b.getTag().toString()));
                }
            }
        }
    }

    private void addDigit(String digit) {
        if (pinBuilder.length() < 4) {
            pinBuilder.append(digit);
            updateDots();
        }

        if (pinBuilder.length() == 4) {
            if (pinBuilder.toString().equals(savedPin)) {
                navigateToDashboard();
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                pinBuilder.setLength(0);
                updateDots();
            }
        }
    }

    private void deleteLastDigit() {
        if (pinBuilder.length() > 0) {
            pinBuilder.setLength(pinBuilder.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setAlpha(i < pinBuilder.length() ? 1.0f : 0.2f);
        }
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                navigateToDashboard();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Quick Access")
                .setSubtitle("Authenticate to open JIJI POS")
                .setNegativeButtonText("Use PIN")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToDashboard() {
        Intent intent = SessionManager.attachSession(this, new Intent(this, DashboardActivity.class));
        startActivity(intent);
        finish();
    }
}
