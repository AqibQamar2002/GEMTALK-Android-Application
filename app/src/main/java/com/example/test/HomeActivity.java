package com.example.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity {

    private TextView quizStatus;
    private Button generateQuizButton;
    private Button continueQuizButton;
    private Button vocabularyButton;
    private Spinner levelSelector;
    private SharedPreferences prefs;
    private String[] questions = new String[10];
    private String[][] options = new String[10][4];
    private int[] correctAnswers = new int[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        quizStatus = findViewById(R.id.quizStatus);
        generateQuizButton = findViewById(R.id.generateQuizButton);
        continueQuizButton = findViewById(R.id.continueQuizButton);
        vocabularyButton = findViewById(R.id.vocabularyButton);
        levelSelector = findViewById(R.id.levelSelector);
        prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE);

        quizStatus.setVisibility(View.GONE);
        updateStatus();

        // Set button colors properly
        setButtonColors();

        // Set both buttons to always be visible
        generateQuizButton.setVisibility(View.VISIBLE);
        continueQuizButton.setVisibility(View.VISIBLE);

        // Generate Quiz Button functionality
        generateQuizButton.setOnClickListener(v -> {
            showQuizTypeDialog();
        });

        // Vocabulary Button functionality
        vocabularyButton.setOnClickListener(v -> {
            String selectedLevel = levelSelector.getSelectedItem().toString();
            Intent intent = new Intent(HomeActivity.this, Vocabulary.class);
            intent.putExtra("quizLevel", selectedLevel);
            startActivity(intent);
        });

        // Continue Quiz Button functionality
        continueQuizButton.setOnClickListener(v -> {
            if (isQuizDataAvailable()) {
                String selectedLevel = levelSelector.getSelectedItem().toString();
                Intent intent = new Intent(HomeActivity.this, QuizActivity.class);
                intent.putExtra("continueQuiz", true);
                intent.putExtra("quizLevel", selectedLevel);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No quiz to continue.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonColors() {
        try {
            // Method 1: Using backgroundTint (Recommended)
            int blueColor = Color.parseColor("#3B82F6");

            generateQuizButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(blueColor)
            );
            continueQuizButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(blueColor)
            );
            vocabularyButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(blueColor)
            );

            // Set text colors to white for better contrast
            generateQuizButton.setTextColor(Color.WHITE);
           // continueQuizButton.setTextColor(Color.WHITE);
            //vocabularyButton.setTextColor(Color.WHITE);

        } catch (Exception e) {
            Log.e("HomeActivity", "Error setting button colors: " + e.getMessage());

            // Fallback method
            try {
                int blueColor = ContextCompat.getColor(this, R.color.primary);
                generateQuizButton.setBackgroundColor(blueColor);
                continueQuizButton.setBackgroundColor(blueColor);
                vocabularyButton.setBackgroundColor(blueColor);
            } catch (Exception ex) {
                Log.e("HomeActivity", "Fallback color setting failed: " + ex.getMessage());
            }
        }
    }

    private void showQuizTypeDialog() {
        String[] quizTypes = getResources().getStringArray(R.array.quiz_types);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quiz Type");
        builder.setItems(quizTypes, (dialog, which) -> {
            String selectedLevel = levelSelector.getSelectedItem().toString();
            String selectedQuizType = quizTypes[which];
            prefs.edit().putBoolean("QuizCompleted", false).apply();
            Intent intent = new Intent(HomeActivity.this, QuizActivity.class);
            intent.putExtra("quizLevel", selectedLevel);
            intent.putExtra("quizType", selectedQuizType);
            startActivity(intent);
        });
        builder.show();
    }

    private void updateStatus() {
        boolean inProgress = prefs.getBoolean("quizInProgress", false);
        if (inProgress) {
            quizStatus.setText("Quiz in progress. Please complete it.");
        } else {
            quizStatus.setText("Ready to generate new quiz.");
        }
    }

    private boolean isQuizDataAvailable() {
        return CheckData();
    }

    private boolean CheckData() {
        return !prefs.getBoolean("QuizCompleted", false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean quizCompleted = getIntent().getBooleanExtra("quizCompleted", false);
        if (quizCompleted) {
            prefs.edit().putBoolean("quizInProgress", false).apply();
            Log.i("MainActivity", "Quiz completed. Flag reset.");
        }

        updateStatus();

        // Reapply colors in case they were lost
        setButtonColors();
    }
}