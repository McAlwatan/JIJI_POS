package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.entity.User;
import com.example.jijipos.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText inputPhone, inputPassword;
    private Button buttonLogin;
    private UserRepository userRepository;
    private TextView textSignUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);

        inputPhone = findViewById(R.id.inputPhone);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textSignUpLink = findViewById(R.id.textSignUpLink);

        // Transition route directly to the Registration Activity view page context
        textSignUpLink.setOnClickListener(v -> {
            Intent secIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(secIntent);
        });

        // Trigger user authentication logic on click
        buttonLogin.setOnClickListener(v -> handleUserAuthentication());
    }

    private void handleUserAuthentication(){
        String phone = inputPhone.getText().toString().trim();
        String rawPassword = inputPassword.getText().toString().trim();

        if(TextUtils.isEmpty(phone)){
            inputPhone.setError("Phone number is required!");
            return;
        }
        if(TextUtils.isEmpty(rawPassword)){
            inputPassword.setError("Password is required!");
            return;
        }

        String encryptedInputPassword = SecurityUtils.hashPassword(rawPassword);

        userRepository.getUserByPhone(phone, user -> {
            runOnUiThread(() -> {
                if(user == null){
                    Toast.makeText(LoginActivity.this, "User record profile not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(user.getPasswordHash().equals(encryptedInputPassword)){
                    Toast.makeText(LoginActivity.this, "Welcome back " + user.getFullName(), Toast.LENGTH_LONG).show();
                    routeUserToDashboard(user); // Triggers our safe, robust navigator routing
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credential mismatch", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void routeUserToDashboard(User user) {
        String role = user.getRole();
        if (role == null) role = "CUSTOMER";

        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);

        // SECURE SESSION EXTRA CARRIER KEYS PASS-THROUGH
        intent.putExtra("USER_ID", user.getId());              // Explicit unique User ID profile tag
        intent.putExtra("USER_NAME", user.getFullName());
        intent.putExtra("USER_ROLE", role.trim().toUpperCase());
        intent.putExtra("USER_PHONE", user.getPhoneNumber());

        // Pass the actual business relation link (Use default 0L fallback for standalone customers)
        long bizId = (user.getBusinessId() != null) ? user.getBusinessId() : 0L;
        intent.putExtra("BUSINESS_ID", bizId);

        startActivity(intent);
        finish();
    }

}
