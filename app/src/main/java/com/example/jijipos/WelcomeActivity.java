package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnSplashLogin = findViewById(R.id.splashLoginBtn);
        Button btnSplashRegister = findViewById(R.id.splashRegisterBtn);

        btnSplashLogin.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSplashRegister.setOnClickListener(v -> {
            // Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            // startActivity(intent);
        });
    }
}
