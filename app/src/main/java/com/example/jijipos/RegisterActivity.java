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

import org.json.JSONObject;

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

        layoutInvitationCode = findViewById(R.id.layoutInvitationCode);
        inputInvitationCode = findViewById(R.id.inputInvitationCode);
        registerName = findViewById(R.id.registerName);
        registerPhone = findViewById(R.id.registerPhone);
        registerPassword = findViewById(R.id.registerPassword);
        spinnerRoles = findViewById(R.id.spinnerRoles);
        buttonRegisterSubmit = findViewById(R.id.buttonRegisterSubmit);

        String[] accountTypes = {"Customer", "Cashier", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(adapter);

        buttonRegisterSubmit.setOnClickListener(v -> processFormSubmission());

        spinnerRoles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String chosenRole = parent.getItemAtPosition(position).toString();
                if (chosenRole.equalsIgnoreCase("CASHIER")) {
                    layoutInvitationCode.setVisibility(View.VISIBLE);
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

        String encryptedPassword = SecurityUtils.hashPassword(rawPassword);

        userRepository.getUserByPhone(phone, existingUser -> {
            runOnUiThread(() -> {
                if (existingUser != null) {
                    Toast.makeText(RegisterActivity.this, "This phone number is already registered!", Toast.LENGTH_LONG).show();
                } else {
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
            User managerProfile = db.userDao().getManagerProfileByPhone(typedTokenCode);

            if (managerProfile == null) {
                runOnUiThread(() -> {
                    inputInvitationCode.setError("Invalid or inactive Manager Invitation Code!");
                });
            } else {
                long associatedBusinessId = managerProfile.getBusinessId();

                User approvedCashier = new User(name, phone, encryptedPassword, "CASHIER", associatedBusinessId);
                db.userDao().insertUser(approvedCashier);

                // =============================================================
                // SUPABASE REAL-TIME CLOUD USER SYNCHRONIZATION OVER-THE-AIR
                // =============================================================
                syncUserToSupabaseCloud(name, phone, encryptedPassword, "CASHIER", associatedBusinessId);
                // =============================================================

                runOnUiThread(() -> {
                    Toast.makeText(this, "Cashier profile created and synced to cloud!", Toast.LENGTH_LONG).show();
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

                // Sync the Business entity block up to cloud table first to satisfy foreign constraints
                syncBusinessToSupabaseCloud(targetedBusinessId, name + " Retail Outlet Store", phone);
            }

            User newUser;
            if (selectedRole.equalsIgnoreCase("MANAGER")) {
                newUser = new User(name, phone, encryptedPassword, selectedRole, targetedBusinessId);
            } else {
                newUser = new User(name, phone, encryptedPassword, selectedRole, null);
            }

            db.userDao().insertUser(newUser);

            // =============================================================
            // SUPABASE REAL-TIME CLOUD USER SYNCHRONIZATION OVER-THE-AIR
            // =============================================================
            Long cloudBizId = (targetedBusinessId > 0) ? targetedBusinessId : null;
            syncUserToSupabaseCloud(name, phone, encryptedPassword, selectedRole, cloudBizId);
            // =============================================================

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Account successfully synced! Please Sign In.", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }

    // Helper method to push Business profiles up to remote PostgreSQL table
    private void syncBusinessToSupabaseCloud(long id, String name, String managerPhone) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id); // Match database schema requirements
            json.put("name", name);
            json.put("location", "Dar es Salaam, TZ");
            json.put("manager_phone", managerPhone);
            json.put("created_at", System.currentTimeMillis());

            SupabaseClient.getInstance().pushRecordToCloud("businesses", json.toString(), new SupabaseClient.CloudSyncCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String err) {}
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper method to push User accounts data models up to remote PostgreSQL table
    private void syncUserToSupabaseCloud(String name, String phone, String passwordHash, String role, Long businessId) {
        try {
            JSONObject json = new JSONObject();
            json.put("full_name", name);
            json.put("phone_number", phone);
            json.put("password_hash", passwordHash);
            json.put("role", role);
            if (businessId != null) {
                json.put("business_id", businessId);
            } else {
                json.put("business_id", JSONObject.NULL);
            }

            SupabaseClient.getInstance().pushRecordToCloud("users", json.toString(), new SupabaseClient.CloudSyncCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String err) {}
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
}
