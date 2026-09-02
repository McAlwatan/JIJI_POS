package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Pause for 2 seconds
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish(); // Wipes MainActivity off the backstack so pressing "Back" doesn't reopen it
        }, 2000);
    }
}
