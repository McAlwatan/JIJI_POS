package com.example.jijipos;

import android.app.AppComponentFactory;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jijipos.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText inputPhone, inputPassword;
    private Button buttonLogin, buttonCancelLogin;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);

        inputPhone = findViewById(R.id.inputLayoutPhone);
        inputPassword = findViewById(R.id.inputLayoutPassword);
        buttonLogin = findViewById(R.id.buttonLogin);


        buttonLogin.setOnClickListener(v -> handleUserAuthentication());


    }
}
