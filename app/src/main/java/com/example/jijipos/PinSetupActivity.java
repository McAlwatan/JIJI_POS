package com.example.jijipos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class PinSetupActivity extends AppCompatActivity {

    private TextInputEditText inputNewPin;
    private Button buttonSavePinSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        inputNewPin = findViewById(R.id.inputNewPin);
        buttonSavePinSubmit = findViewById(R.id.buttonSavePinSubmit);

        buttonSavePinSubmit.setOnClickListener(v -> {
            String pin = inputNewPin.getText().toString().trim();
            if (TextUtils.isEmpty(pin) || pin.length() < 4) {
                inputNewPin.setError("PIN must be exactly 4 digits!");
                return;
            }

            // Save the PIN securely to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("JIJI_POS_SECURITY_PREFS", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("SECURITY_PIN_KEY", pin);
            editor.apply();

            // Arriving straight from registration: persist the full session now
            Intent incoming = getIntent();
            if (incoming.hasExtra("USER_ID")) {
                SessionManager.saveSession(this,
                        incoming.getLongExtra("USER_ID", 0L),
                        incoming.getStringExtra("USER_NAME"),
                        incoming.getStringExtra("USER_PHONE"),
                        incoming.getStringExtra("USER_ROLE"),
                        incoming.getLongExtra("BUSINESS_ID", 0L));
            }

            Toast.makeText(this, "Access PIN established successfully!", Toast.LENGTH_SHORT).show();

            // Direct the user immediately to DashboardActivity with the full session stamped
            Intent intent = SessionManager.attachSession(this, new Intent(PinSetupActivity.this, DashboardActivity.class));
            startActivity(intent);
            finish();
        });
    }
}
