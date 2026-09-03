package com.example.jijipos;

import android.app.AppComponentFactory;
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
    private Button buttonLogin, buttonCancelLogin;
    private UserRepository userRepository;

    private TextView textSignUpLink;

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);

        inputPhone = findViewById(R.id.inputPhone);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textSignUpLink = findViewById(R.id.textSignUpLink);

        textSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });


        buttonLogin.setOnClickListener(v -> handleUserAuthentication());


    }

    private void handleUserAuthentication(){
        String phone = inputPhone.getText().toString().trim();
        String rawPassword = inputPassword.getText().toString().trim();

        if(TextUtils.isEmpty(phone)){
            inputPhone.setError("Phone number is required!");
        }
        if(TextUtils.isEmpty(rawPassword)){
            inputPassword.setError("Password is required!");
        }


        String encryptedInputPassword = SecurityUtils.hashPassword(rawPassword);

        userRepository.getUserByPhone(phone, user -> {

            runOnUiThread(() -> {
                if(user == null){
                    Toast.makeText(LoginActivity.this, "User record profile not found!", Toast.LENGTH_SHORT).show();
                }

                if(user.getPasswordHash().equals(encryptedInputPassword)){
                    Toast.makeText(LoginActivity.this, "Welcome back" + user.getFullName(), Toast.LENGTH_LONG).show();
                    routeUserToDashboard(user);
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

        intent.putExtra("USER_NAME", user.getFullName());
        intent.putExtra("USER_ROLE", role.trim().toUpperCase());
        switch (role.toUpperCase().trim()) {
            case "CUSTOMER":
                Toast.makeText(LoginActivity.this, "Routing to Customer space...", Toast.LENGTH_SHORT).show();
                break;
            case "CASHIER":
                Toast.makeText(LoginActivity.this, "Routing to Cashier workspace...", Toast.LENGTH_SHORT).show();
                break;
            case "MANAGER":
                Toast.makeText(LoginActivity.this, "Routing to Manager workspace...", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(LoginActivity.this, "System admin bypass access unconfigured.", Toast.LENGTH_SHORT).show();
                break;
        }

        // 4. Launch the intent transaction and wipe the Login activity off the memory stack
        startActivity(intent);
        finish();
    }


}
