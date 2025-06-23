package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private ImageView splashLogo;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        splashLogo = findViewById(R.id.splash_logo);
        loadingText = findViewById(R.id.loading_text);

        // Add fade-in animation to logo
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(fadeIn);

        // Add slide-up animation to loading text
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        loadingText.startAnimation(slideUp);

        // Navigate to MainActivity after splash duration
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Close splash activity

                // Add transition animation
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back button on splash screen
        // Do nothing
        super.onBackPressed();
    }
}