package com.example.jijipos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animateSplash();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }

    private void animateSplash() {
        View logo = findViewById(R.id.splashLogo);
        View appName = findViewById(R.id.splashAppName);
        View tagline = findViewById(R.id.splashTagline);

        fadeInScale(logo, 0, 600);
        fadeInUp(appName, 250);
        fadeInUp(tagline, 400);
    }

    private void fadeInScale(View view, long delay, long duration) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void fadeInUp(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
