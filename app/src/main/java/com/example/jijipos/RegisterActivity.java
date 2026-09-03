package com.example.jijipos;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.entity.User;
import com.example.jijipos.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText registerName, registerPhone, registerPassword;
    private Spinner spinnerRoles;
    private Button buttonRegisterSubmit, buttonBackToLogin;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);

        registerName = findViewById(R.id.registerName);
        registerPhone = findViewById(R.id.registerPhone);
        registerPassword = findViewById(R.id.registerPassword);
        spinnerRoles = findViewById(R.id.spinnerRoles);
        buttonRegisterSubmit = findViewById(R.id.buttonRegisterSubmit);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        String[] accountTypes = {"Customer", "Cashier", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(adapter);
        buttonRegisterSubmit.setOnClickListener(v -> processFormSubmission());
        buttonBackToLogin.setOnClickListener(v -> finish());
    }

    private void processFormSubmission() {
        String name = registerName.getText().toString().trim();
        String phone = registerPhone.getText().toString().trim();
        String rawPassword = registerPassword.getText().toString().trim();
        String selectedRole = spinnerRoles.getSelectedItem().toString().toUpperCase();

        if (TextUtils.isEmpty(name)) {
            registerName.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            registerPhone.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(rawPassword) || rawPassword.length() < 4) {
            registerPassword.setError("Password must be at least 4 characters long");
            return;
        }

        String encryptedPassword = SecurityUtils.hashPassword(rawPassword);

        userRepository.getUserByPhone(phone, existingUser -> {
            runOnUiThread(() -> {
                if (existingUser != null) {
                    Toast.makeText(RegisterActivity.this, "This phone number is already registered!", Toast.LENGTH_LONG).show();
                } else {
                    // Create structural entity user object payload to commit to Room storage
                    // Set parent business constraint to null for early standalone sign-ups
                    User newUser = new User(name, phone, encryptedPassword, selectedRole, null);
                    
                    userRepository.insertUser(newUser, newId -> {
                        runOnUiThread(() -> {
                            if (newId > 0) {
                                Toast.makeText(RegisterActivity.this, "Account created successfully! Please Sign In.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration failed, please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                }
            });
        });
    }
}
