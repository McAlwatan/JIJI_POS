package com.example.jijipos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Business;
import com.example.jijipos.database.entity.User;
import com.example.jijipos.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;

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
                    // Not on this device yet: fall back to the cloud so an
                    // account registered elsewhere can still sign in online.
                    attemptCloudLogin(phone, encryptedInputPassword);
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

    /**
     * Online login fallback. When the profile is absent from the local Room DB
     * (e.g. a fresh device), pull it from Supabase, verify the password hash,
     * cache the profile locally and continue into the normal routing.
     */
    private void attemptCloudLogin(String phone, String encryptedInputPassword) {
        Toast.makeText(this, "Checking your online profile...", Toast.LENGTH_SHORT).show();
        buttonLogin.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String json = SupabaseClient.getInstance().fetchUserJsonByPhone(phone);
                JSONArray array = (json != null) ? new JSONArray(json) : new JSONArray();

                if (array.length() == 0) {
                    runOnUiThread(() -> {
                        buttonLogin.setEnabled(true);
                        Toast.makeText(this, "No account found for this phone number", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONObject obj = array.getJSONObject(0);
                String cloudHash = obj.optString("password_hash");
                if (!encryptedInputPassword.equals(cloudHash)) {
                    runOnUiThread(() -> {
                        buttonLogin.setEnabled(true);
                        Toast.makeText(this, "Invalid credential mismatch", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String name = obj.optString("full_name");
                String role = obj.optString("role", "CUSTOMER");
                Long businessId = obj.isNull("business_id") ? null : obj.optLong("business_id");

                AppDatabase db = AppDatabase.getInstance(this);

                // Preserve the manager/cashier business linkage by mirroring the
                // cloud business row locally first (avoids a foreign-key failure).
                Long localBusinessId = null;
                if (businessId != null && businessId > 0) {
                    Business existing = db.businessDao().getBusinessById(businessId);
                    if (existing != null) {
                        localBusinessId = businessId;
                    } else {
                        String bJson = SupabaseClient.getInstance().fetchBusinessJsonById(businessId);
                        JSONArray bArray = (bJson != null) ? new JSONArray(bJson) : new JSONArray();
                        if (bArray.length() > 0) {
                            JSONObject bObj = bArray.getJSONObject(0);
                            Business business = new Business(
                                    bObj.optString("name"),
                                    bObj.optString("location"),
                                    bObj.optString("manager_phone"),
                                    bObj.optLong("created_at", System.currentTimeMillis()));
                            business.setId(businessId);
                            db.businessDao().insertBusiness(business);
                            localBusinessId = businessId;
                        }
                    }
                }

                User cloudUser = new User(name, phone, cloudHash, role, localBusinessId);
                long newId = db.userDao().insertUser(cloudUser);
                User saved = db.userDao().getUserById(newId);

                runOnUiThread(() -> {
                    buttonLogin.setEnabled(true);
                    if (saved != null) {
                        Toast.makeText(this, "Profile synced to this device. Welcome " + saved.getFullName(), Toast.LENGTH_LONG).show();
                        routeUserToDashboard(saved);
                    } else {
                        Toast.makeText(this, "Could not save the synced profile", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    buttonLogin.setEnabled(true);
                    Toast.makeText(this, "Could not reach the cloud. Check your connection and try again.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void routeUserToDashboard(User user) {
        String role = user.getRole();
        if (role == null) role = "CUSTOMER";
        role = role.trim().toUpperCase();

        // Cache the full session so every downstream screen (PIN lock, PIN
        // setup, dashboard, staff list, reports) reads one source of truth
        SessionManager.saveSession(this, user.getId(), user.getFullName(), user.getPhoneNumber(),
                role, (user.getBusinessId() != null) ? user.getBusinessId() : 0L);

        if (SessionManager.hasPin(this)) {
            Intent intent = new Intent(LoginActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Intent intent = SessionManager.attachSession(this, new Intent(LoginActivity.this, PinSetupActivity.class));
        startActivity(intent);
        finish();
    }

}
