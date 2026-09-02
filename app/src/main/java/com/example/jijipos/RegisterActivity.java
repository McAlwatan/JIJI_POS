package com.example.jijipos;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private Spinner spinnerRoles;
    private Button buttonBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        spinnerRoles = findViewById(R.id.spinnerRoles);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        String[] accountType = {"Customer", "Cashier", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, accountType);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinnerRoles.setAdapter(adapter);

        buttonBackToLogin.setOnClickListener(v -> finish());
    }
}
