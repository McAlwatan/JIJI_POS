package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Business;
import com.example.jijipos.database.entity.User;
import com.example.jijipos.repository.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText registerName, registerPhone, registerPassword, inputInvitationCode, inputOtpCode;
    private TextInputLayout layoutInvitationCode;
    private Spinner spinnerRoles;
    private Button buttonRegisterSubmit, buttonOtpVerifySubmit;
    private MaterialCardView cardOtpOverlay;
    private TextView textOtpSubLabel, textOtpCountdown, textOtpCancel;
    private UserRepository userRepository;

    private FirebaseAuth firebaseAuth;
    private String currentVerificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Registration details held while the phone number is being verified
    private String pendingName, pendingPhone, pendingEncryptedPassword, pendingRole;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);
        firebaseAuth = FirebaseAuth.getInstance();

        layoutInvitationCode = findViewById(R.id.layoutInvitationCode);
        inputInvitationCode = findViewById(R.id.inputInvitationCode);
        registerName = findViewById(R.id.registerName);
        registerPhone = findViewById(R.id.registerPhone);
        registerPassword = findViewById(R.id.registerPassword);
        spinnerRoles = findViewById(R.id.spinnerRoles);
        buttonRegisterSubmit = findViewById(R.id.buttonRegisterSubmit);

        // Bind OTP Components cleanly
        cardOtpOverlay = findViewById(R.id.cardOtpOverlay);
        inputOtpCode = findViewById(R.id.inputOtpCode);
        buttonOtpVerifySubmit = findViewById(R.id.buttonOtpVerifySubmit);
        textOtpSubLabel = findViewById(R.id.textOtpSubLabel);
        textOtpCountdown = findViewById(R.id.textOtpCountdown);
        textOtpCancel = findViewById(R.id.textOtpCancel);

        String[] accountTypes = {"Customer", "Cashier", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(adapter);

        buttonRegisterSubmit.setOnClickListener(v -> processFormSubmission());

        buttonOtpVerifySubmit.setOnClickListener(v -> verifyEnteredOtpCode());
        textOtpCancel.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            cardOtpOverlay.setVisibility(View.GONE);
        });
        textOtpCountdown.setOnClickListener(v -> {
            if (pendingPhone == null) return;
            textOtpCountdown.setClickable(false);
            textOtpCountdown.setTextColor(getResources().getColor(R.color.text_muted));
            initiateOtpFlow(pendingName, pendingPhone, pendingEncryptedPassword, pendingRole, true);
        });

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
                    initiateOtpFlow(name, phone, encryptedPassword, selectedRole, false);
                }
            });
        });
    }

    private void initiateOtpFlow(String name, String phone, String encryptedPassword, String selectedRole, boolean isResend) {
        pendingName = name;
        pendingPhone = phone;
        pendingEncryptedPassword = encryptedPassword;
        pendingRole = selectedRole;

        String e164Phone = toE164Phone(phone);
        textOtpSubLabel.setText("We've sent a 6-digit verification code to " + phone + ". Please enter it below to complete your registration.");

        // --- REAL SMS DELIVERY + SERVER-SIDE VERIFICATION VIA FIREBASE PHONE AUTH ---
        PhoneAuthOptions.Builder optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(e164Phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Instant validation or automatic SMS code retrieval
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e("JIJI_POS_OTP", "Firebase verification failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                registerPhone.setError("Enter a valid phone number with country code (e.g. +255...)");
                            }
                            Toast.makeText(RegisterActivity.this, "OTP send failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        currentVerificationId = verificationId;
                        resendToken = token;
                        Log.d("JIJI_POS_OTP", "Firebase OTP SMS dispatched to " + e164Phone);
                        runOnUiThread(() -> {
                            cardOtpOverlay.setVisibility(View.VISIBLE);
                            inputOtpCode.setText("");
                            startResendCountdown();
                        });
                    }
                });
        if (isResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken);
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build());
        // -----------------------------------------------------------------------------
    }

    private void startResendCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                textOtpCountdown.setText("Resend code in 00:" + String.format("%02d", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                textOtpCountdown.setText("Resend Code");
                textOtpCountdown.setTextColor(getResources().getColor(R.color.teal_light));
                textOtpCountdown.setClickable(true);
            }
        }.start();
    }

    private void verifyEnteredOtpCode() {
        String typedCode = inputOtpCode.getText().toString().trim();
        if (currentVerificationId == null) {
            inputOtpCode.setError("Verification code not sent yet, please wait!");
            return;
        }
        if (typedCode.length() != 6) {
            inputOtpCode.setError("Enter the 6-digit code sent to your phone!");
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(currentVerificationId, typedCode);
        signInWithPhoneCredential(credential);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                Log.e("JIJI_POS_OTP", "OTP verification rejected: " + task.getException());
                runOnUiThread(() -> inputOtpCode.setError("Invalid OTP code, please check and try again!"));
                return;
            }
            // Firebase is used purely as the OTP provider: drop its session afterwards
            firebaseAuth.signOut();
            if (countDownTimer != null) countDownTimer.cancel();
            cardOtpOverlay.setVisibility(View.GONE);

            if ("CASHIER".equalsIgnoreCase(pendingRole)) {
                handleCashierVerificationAndSignUp(pendingName, pendingPhone, pendingEncryptedPassword);
            } else {
                handleStandardUserSignUp(pendingName, pendingPhone, pendingEncryptedPassword, pendingRole);
            }
        });
    }

    // Firebase requires E.164 format; default country code is Tanzania (+255)
    private String toE164Phone(String phone) {
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("00")) return "+" + digits.substring(2);
        if (digits.startsWith("0")) return "+255" + digits.substring(1);
        return "+255" + digits;
    }

    private void handleCashierVerificationAndSignUp(String name, String phone, String encryptedPassword) {
        String typedTokenCode = inputInvitationCode.getText().toString().trim();

        if (TextUtils.isEmpty(typedTokenCode)) {
            inputInvitationCode.setError("You need a secure Manager Code to register as a Cashier!");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            boolean managerFound = false;
            Long associatedBusinessId = null;

            // 1) Try this device first
            User managerProfile = db.userDao().getManagerProfileByPhone(typedTokenCode);
            if (managerProfile != null) {
                managerFound = true;
                associatedBusinessId = managerProfile.getBusinessId();
            } else {
                // 2) Fall back to Supabase so a manager registered on another device is still valid
                try {
                    String json = SupabaseClient.getInstance().fetchManagerJsonByPhone(typedTokenCode);
                    JSONArray array = (json != null) ? new JSONArray(json) : new JSONArray();
                    if (array.length() > 0) {
                        JSONObject obj = array.getJSONObject(0);
                        managerFound = true;
                        if (!obj.isNull("business_id")) {
                            associatedBusinessId = obj.optLong("business_id");
                        }
                        Log.d("JIJI_POS_SYNC", "Manager invite validated from cloud for " + typedTokenCode);
                    }
                } catch (Exception e) {
                    Log.e("JIJI_POS_SYNC", "Manager cloud lookup failed: " + e.getMessage());
                }
            }

            if (!managerFound) {
                runOnUiThread(() -> inputInvitationCode.setError("Invalid or inactive Manager Invitation Code!"));
                return;
            }

            // 3) Mirror the manager's business locally so the cashier's foreign key holds on this device
            final Long cashierBusinessId = ensureLocalBusiness(db, associatedBusinessId);

            User approvedCashier = new User(name, phone, encryptedPassword, "CASHIER", cashierBusinessId);
            long cashierUserId = db.userDao().insertUser(approvedCashier);
            SessionManager.saveSession(this, cashierUserId, name, phone, "CASHIER",
                    cashierBusinessId != null ? cashierBusinessId : 0L);

            // =============================================================
            // SUPABASE REAL-TIME CLOUD USER SYNCHRONIZATION OVER-THE-AIR
            // =============================================================
            syncUserToSupabaseCloud(name, phone, encryptedPassword, "CASHIER", cashierBusinessId);
            // =============================================================

            runOnUiThread(() -> {
                Toast.makeText(this, "Cashier profile created and synced to cloud!", Toast.LENGTH_LONG).show();

                // Route directly to PIN Setup with the full session stamped
                Intent intent = SessionManager.attachSession(this, new Intent(RegisterActivity.this, PinSetupActivity.class));
                startActivity(intent);
                finish();
            });
        });
    }

    /**
     * Guarantees a Business row exists locally for the given cloud business id so
     * the cashier's {@code businessId} foreign key can be satisfied on this device.
     * Mirrors the cloud business when present; otherwise inserts a placeholder that
     * preserves the id/linkage. Returns null only when there is no business id at all.
     */
    private Long ensureLocalBusiness(AppDatabase db, Long businessId) {
        if (businessId == null || businessId <= 0) return null;

        Business existing = db.businessDao().getBusinessById(businessId);
        if (existing != null) return businessId;

        try {
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
                return businessId;
            }
        } catch (Exception e) {
            Log.e("JIJI_POS_SYNC", "Business cloud lookup failed: " + e.getMessage());
        }

        // Last resort: keep the linkage with a local placeholder carrying the same id
        Business placeholder = new Business("Manager Business", "Unknown", "", System.currentTimeMillis());
        placeholder.setId(businessId);
        db.businessDao().insertBusiness(placeholder);
        return businessId;
    }

    private void handleStandardUserSignUp(String name, String phone, String encryptedPassword, String selectedRole) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long targetedBusinessId = 0L;

            if (selectedRole.equalsIgnoreCase("MANAGER")) {
                Business newEnterprise = new Business(
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

            long registeredUserId = db.userDao().insertUser(newUser);
            SessionManager.saveSession(this, registeredUserId, name, phone, selectedRole, targetedBusinessId);
            Long cloudBizId = (targetedBusinessId > 0) ? targetedBusinessId : null;
            syncUserToSupabaseCloud(name, phone, encryptedPassword, selectedRole, cloudBizId);

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Account successfully synced! Please establish your access PIN.", Toast.LENGTH_LONG).show();
                
                // Route directly to PIN Setup with the full session stamped
                Intent intent = SessionManager.attachSession(this, new Intent(RegisterActivity.this, PinSetupActivity.class));
                startActivity(intent);
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
                @Override public void onSuccess() {
                    Log.d("JIJI_POS_SYNC", "Business profile synced to cloud: id=" + id);
                }
                @Override public void onFailure(String err) {
                    Log.e("JIJI_POS_SYNC", "Business cloud sync FAILED for id=" + id + ": " + err);
                }
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
                @Override public void onSuccess() {
                    Log.d("JIJI_POS_SYNC", "User profile synced to cloud: " + phone);
                }
                @Override public void onFailure(String err) {
                    Log.e("JIJI_POS_SYNC", "User cloud sync FAILED for " + phone + ": " + err
                            + " (cross-device login needs this row readable in Supabase)");
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
}
