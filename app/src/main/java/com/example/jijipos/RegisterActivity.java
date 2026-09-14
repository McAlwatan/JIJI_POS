package com.example.jijipos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.User;
import com.example.jijipos.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText registerName, registerPhone, registerPassword, inputInvitationCode;
    private TextInputLayout layoutInvitationCode;
    private Spinner spinnerRoles;
    private Button buttonRegisterSubmit;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);

        // Bind layouts and input widgets
        layoutInvitationCode = findViewById(R.id.layoutInvitationCode);
        inputInvitationCode = findViewById(R.id.inputInvitationCode);
        registerName = findViewById(R.id.registerName);
        registerPhone = findViewById(R.id.registerPhone);
        registerPassword = findViewById(R.id.registerPassword);
        spinnerRoles = findViewById(R.id.spinnerRoles);
        buttonRegisterSubmit = findViewById(R.id.buttonRegisterSubmit);

        // Populate dynamic role options dropdown
        String[] accountTypes = {"Customer", "Cashier", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(adapter);

        // Map submission click action
        buttonRegisterSubmit.setOnClickListener(v -> processFormSubmission());

        // Toggle invitation input field visibility depending on selected spinner option choice
        spinnerRoles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String chosenRole = parent.getItemAtPosition(position).toString();
                if (chosenRole.equalsIgnoreCase("CASHIER")) {
                    layoutInvitationCode.setVisibility(View.VISIBLE); // Reveal code input
                } else {
                    layoutInvitationCode.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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

        // 2. Encryption Hash Processing
        String encryptedPassword = SecurityUtils.hashPassword(rawPassword);

        // 3. Check for Duplicate Mobile Registrations
        userRepository.getUserByPhone(phone, existingUser -> {
            runOnUiThread(() -> {
                if (existingUser != null) {
                    Toast.makeText(RegisterActivity.this, "This phone number is already registered!", Toast.LENGTH_LONG).show();
                } else {
                    // Execute Branching paths depending on User Type Account Permissions Rules
                    if (selectedRole.equalsIgnoreCase("CASHIER")) {
                        handleCashierVerificationAndSignUp(name, phone, encryptedPassword);
                    } else {
                        handleStandardUserSignUp(name, phone, encryptedPassword, selectedRole);
                    }
                }
            });
        });
    }

    private void handleCashierVerificationAndSignUp(String name, String phone, String encryptedPassword) {
        String typedTokenCode = inputInvitationCode.getText().toString().trim();

        if (TextUtils.isEmpty(typedTokenCode)) {
            inputInvitationCode.setError("You need a secure Manager Code to register as a Cashier!");
            return;
        }

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // 1. Fetch the actual Manager profile matching the invitation phone code token
            User managerProfile = db.userDao().getManagerProfileByPhone(typedTokenCode);

            if (managerProfile == null) {
                // If no Manager exists with that phone number, reject registration safely without crashing
                runOnUiThread(() -> {
                    inputInvitationCode.setError("Invalid or inactive Manager Invitation Code! Please check the number.");
                });
            } else {
                // 2. Extract the manager's valid business relationship context ID safely
                long associatedBusinessId = managerProfile.getBusinessId();

                // 3. Create and persist the verified Cashier sub_account linked directly to the store context
                User approvedCashier = new User(name, phone, encryptedPassword, "CASHIER", associatedBusinessId);
                db.userDao().insertUser(approvedCashier);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Cashier profile successfully verified and linked!", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void handleStandardUserSignUp(String name, String phone, String encryptedPassword, String selectedRole) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long targetedBusinessId = 0L;

            if (selectedRole.equalsIgnoreCase("MANAGER")) {
                com.example.jijipos.database.entity.Business newEnterprise = new com.example.jijipos.database.entity.Business(
                        name + " Retail Outlet Store", "Dar es Salaam, TZ", phone, System.currentTimeMillis()
                );
                targetedBusinessId = db.businessDao().insertBusiness(newEnterprise);
            }

            User newUser;
            if (selectedRole.equalsIgnoreCase("MANAGER")) {
                newUser = new User(name, phone, encryptedPassword, selectedRole, targetedBusinessId);
            } else {
                newUser = new User(name, phone, encryptedPassword, selectedRole, null);
            }

            db.userDao().insertUser(newUser);

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Account created successfully! Please Sign In.", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}
