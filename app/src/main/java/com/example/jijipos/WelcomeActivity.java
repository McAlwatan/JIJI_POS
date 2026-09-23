package com.example.jijipos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Intercept session check for returning users
        SharedPreferences prefs = getSharedPreferences("JIJI_POS_SECURITY_PREFS", Context.MODE_PRIVATE);
        if (prefs.contains("SECURITY_PIN_KEY")) {
            startActivity(new Intent(WelcomeActivity.this, PinLockActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        Button btnSplashLogin = findViewById(R.id.splashLoginBtn);
        Button btnSplashRegister = findViewById(R.id.splashRegisterBtn);

        btnSplashLogin.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSplashRegister.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
