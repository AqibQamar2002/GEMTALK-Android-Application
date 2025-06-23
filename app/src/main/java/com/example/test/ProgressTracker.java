package com.example.test;

import android.content.SharedPreferences;

public class ProgressTracker {

    public static void saveProgress(SharedPreferences prefs, int correctAnswers) {
        int totalCorrect = prefs.getInt("totalCorrect", 0);
        prefs.edit().putInt("totalCorrect", totalCorrect + correctAnswers).apply();
    }

    public static int getProgress(SharedPreferences prefs) {
        return prefs.getInt("totalCorrect", 0);
    }
}