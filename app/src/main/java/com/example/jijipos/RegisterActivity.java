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

    // Handles Invitation Verification checks for Restricted Cashier accounts
    private void handleCashierVerificationAndSignUp(String name, String phone, String encryptedPassword) {
        String typedTokenCode = inputInvitationCode.getText().toString().trim();

        if (TextUtils.isEmpty(typedTokenCode)) {
            inputInvitationCode.setError("You need a secure Manager Code to register as a Cashier!");
            return;
        }

        // Run validation against token keys database mapping inside independent worker thread lanes safely
        // Run validation against token keys database mapping inside independent worker thread lanes safely
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Long associatedBusinessIdObj = db.userDao().verifyManagerInvitationToken(typedTokenCode);

            // =============================================================
            // DEVELOPMENT MOCK BYPASS (ENFORCES STABLE ISOLATED TESTING)
            // =============================================================
            if (associatedBusinessIdObj == null && typedTokenCode.equals("0810101010")) {
                // If code is not found in database yet, automatically assign it to business 1L for testing
                associatedBusinessIdObj = 1L;
            }
            // =============================================================

            if (associatedBusinessIdObj == null) {
                runOnUiThread(() -> {
                    inputInvitationCode.setError("Invalid or expired Manager Invitation Code!");
                });
            } else {
                long associatedBusinessId = associatedBusinessIdObj;

                // Instantiate customized corporate worker user instance explicitly linked to verified store reference index
                User approvedCashier = new User(name, phone, encryptedPassword, "CASHIER", associatedBusinessId);

                db.userDao().insertUser(approvedCashier);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Cashier profile successfully verified and linked!", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    // Handles standard registration loops for default open roles (Customers/Managers)
    private void handleStandardUserSignUp(String name, String phone, String encryptedPassword, String selectedRole) {
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
}
