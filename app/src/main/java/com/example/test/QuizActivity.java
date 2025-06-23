package com.example.test;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class QuizActivity extends AppCompatActivity {

    private TextView questionView, questionCounter;
    private Button optionA, optionB, optionC, optionD, nextButton, backToHomeButton;
    private String[] questions = new String[10];
    private String[][] options = new String[10][4];
    private int[] correctAnswers = new int[10];
    private int currentIndex = 0;
    private int correctCount = 0;
    private SharedPreferences prefs;
    private final OkHttpClient client = new OkHttpClient();
    private ProgressDialog progressDialog;
    private boolean isActivityActive = true;
    private ProgressBar progressBar;

    // Color constants
    private static final String PRIMARY_BLUE = "#3B82F6";
    private static final String CORRECT_GREEN = "#10B981";
    private static final String WRONG_RED = "#EF4444";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Initialize views
        questionView = findViewById(R.id.questionView);
        questionCounter = findViewById(R.id.questionCounter);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        nextButton = findViewById(R.id.nextButton);
        backToHomeButton = findViewById(R.id.backToHomeButton);
        progressBar = findViewById(R.id.progressBar);

        prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE);

        // Set initial button colors and text
        setInitialButtonStyles();

        // Initialize progress bar
        initializeProgressBar();

        boolean continueQuiz = getIntent().getBooleanExtra("continueQuiz", false);
        if (continueQuiz) {
            restoreQuizState();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading quiz...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            fetchQuizFromAPI();
        }

        backToHomeButton.setOnClickListener(v -> {
            prefs.edit().putBoolean("quizInProgress", false).apply();
            Intent intent = new Intent(QuizActivity.this, HomeActivity.class);
            if(currentIndex >= 9) {
                intent.putExtra("quizCompleted", true);
            }
            startActivity(intent);
            finish();
        });
    }

    private void initializeProgressBar() {
        if (progressBar != null) {
            progressBar.setMax(10); // Total questions
            progressBar.setProgress(currentIndex + 1); // Current progress

            // Set progress bar colors
            try {
                progressBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor(PRIMARY_BLUE))
                );
                progressBar.setProgressBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                );
            } catch (Exception e) {
                Log.e("QuizActivity", "Error setting progress bar colors: " + e.getMessage());
            }
        }
    }

    private void updateProgress() {
        if (progressBar != null) {
            progressBar.setProgress(currentIndex + 1);
        }

        if (questionCounter != null) {
            questionCounter.setText((currentIndex + 1) + "/10");
        }
    }

    private void setInitialButtonStyles() {
        // Set blue background and white text for all option buttons
        setButtonStyle(optionA, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionB, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionC, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionD, PRIMARY_BLUE, Color.WHITE);

        // Set action buttons
        setButtonStyle(nextButton, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(backToHomeButton, PRIMARY_BLUE, Color.BLACK);
    }

    private void setButtonStyle(Button button, String backgroundColor, int textColor) {
        try {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(backgroundColor))
            );
            button.setTextColor(textColor);
        } catch (Exception e) {
            Log.e("QuizActivity", "Error setting button style: " + e.getMessage());
            // Fallback
            button.setBackgroundColor(Color.parseColor(backgroundColor));
            button.setTextColor(textColor);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveQuizState();
    }

    @Override
    protected void onDestroy() {
        isActivityActive = false;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        prefs.edit().putBoolean("quizInProgress", false).apply();
        super.onDestroy();
    }

    private String getQuizLevel1() {
        String level = getIntent().getStringExtra("quizLevel");
        if (level=="Beginner") {
            return "Easy";
        } else if (level=="Intermediate") {
            return "Medium";
        } else if (level=="Advanced") {
            return "Hard";
        } else {
            return "Medium";
        }
    }

    private void fetchQuizFromAPI() {
        String quizLevel = getIntent().getStringExtra("quizLevel");
        String quizType = getIntent().getStringExtra("quizType");
        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject contentObject = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject partObject = new JSONObject();

            if (quizType.equals("Fill in the Blanks")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language fill-in-the-blank questions. Each question must have a blank to fill and 4 options, with one correct answer. Ensure the vocabulary used is common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the types of sentences and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the sentence with a blank.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else if (quizType.equals("Synonyms")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language synonym questions. Each question must ask for the synonym of a word and provide 4 options, with one correct answer. Ensure the vocabulary used is common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the words and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the word for which the synonym is required.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else if (quizType.equals("Antonyms")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language antonym questions. Each question must ask for the antonym of a word and provide 4 options, with one correct answer. Ensure the vocabulary used is common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the words and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the word for which the antonym is required.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else if (quizType.equals("Sentence Correction")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language sentence correction questions. Each question must present a sentence with an error and provide 4 correction options, with one correct answer. Ensure the vocabulary and grammar used are common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the types of errors and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the sentence with an error.\n" +
                        "- 'options': An array of 4 strings representing the correction choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else if (quizType.equals("Parts of Speech")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language parts of speech questions. Each question must ask to identify the part of speech of a word in a sentence and provide 4 options, with one correct answer. Ensure the vocabulary and grammar used are common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the types of sentences and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the sentence with the word to identify.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else if (quizType.equals("Idioms and Phrases")) {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language idioms and phrases questions. Each question must ask for the meaning of an idiom or phrase and provide 4 options, with one correct answer. Ensure the idioms and phrases used are common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the idioms and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the idiom or phrase.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            } else {
                partObject.put("text", "Generate 10 " + quizLevel.toLowerCase() + " level English language multiple-choice questions. Each question must have 4 options and one correct answer. Ensure the vocabulary used in questions and options is common and familiar for the specified level, avoiding obscure or overly academic terms. Strive for variety in the types of questions and topics generated with each request. Return the questions as a JSON array of objects, where each object has:\n" +
                        "- 'question': A string containing the MCQ question.\n" +
                        "- 'options': An array of 4 strings representing the answer choices.\n" +
                        "- 'answerIndex': An integer (0, 1, 2, or 3) indicating the 0-based index of the correct answer within the 'options' array.");
            }
            parts.put(partObject);
            contentObject.put("parts", parts);
            contents.put(contentObject);
            jsonBody.put("contents", contents);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=<Your_API_Key>")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (isActivityActive) {
                        progressDialog.dismiss();
                        questionView.setText("Error: " + e.getMessage());
                        Log.e("QuizActivity", "API call failed: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (isActivityActive) {
                            progressDialog.dismiss();
                            questionView.setText("Error: " + response.message());
                            Log.e("QuizActivity", "API response unsuccessful: " + response.message());
                        }
                    });
                    return;
                }
                try {
                    String responseBodyString = response.body().string();
                    Log.d("QuizActivity", "Response Body: " + responseBodyString);

                    JSONObject body = new JSONObject(responseBodyString);
                    JSONArray candidates = body.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        throw new JSONException("Missing 'candidates'");
                    }

                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.optJSONObject("content");
                    if (content == null) {
                        throw new JSONException("Missing 'content'");
                    }

                    JSONArray parts = content.optJSONArray("parts");
                    if (parts == null || parts.length() == 0) {
                        throw new JSONException("Missing 'parts'");
                    }

                    String text = parts.getJSONObject(0).getString("text");
                    text = text.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "");

                    JSONArray quizArray = new JSONArray(text);

                    if (quizArray.length() != 10) {
                        throw new JSONException("Invalid quiz format");
                    }

                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject q = quizArray.getJSONObject(i);
                        questions[i] = q.getString("question");
                        JSONArray opt = q.getJSONArray("options");
                        for (int j = 0; j < 4; j++) {
                            options[i][j] = opt.getString(j);
                        }
                        correctAnswers[i] = q.getInt("answerIndex");
                    }

                    prefs.edit().putBoolean("quizInProgress", true).apply();
                    saveQuizState();

                    runOnUiThread(() -> {
                        if (isActivityActive) {
                            progressDialog.dismiss();
                            displayQuestion();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        if (isActivityActive) {
                            progressDialog.dismiss();
                            prefs.edit().putBoolean("quizInProgress", false).apply();
                            questionView.setText("Error parsing questions.");
                            Log.e("QuizActivity", "Error parsing questions: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void displayQuestion() {
        if (currentIndex < 10) {
            questionView.setText(questions[currentIndex]);
            optionA.setText(options[currentIndex][0]);
            optionB.setText(options[currentIndex][1]);
            optionC.setText(options[currentIndex][2]);
            optionD.setText(options[currentIndex][3]);

            // Update progress bar and counter
            updateProgress();

            // Reset to blue buttons with white text
            resetButtonColors();
            enableOptionButtons(true);
            nextButton.setVisibility(View.GONE);

            View.OnClickListener listener = v -> {
                int selected = 0;
                if (v == optionA) selected = 0;
                else if (v == optionB) selected = 1;
                else if (v == optionC) selected = 2;
                else if (v == optionD) selected = 3;

                if (selected == correctAnswers[currentIndex]) {
                    // Correct answer - turn green
                    setButtonStyle((Button) v, CORRECT_GREEN, Color.WHITE);
                    correctCount++;
                } else {
                    // Wrong answer - turn red
                    setButtonStyle((Button) v, WRONG_RED, Color.WHITE);
                    // Also highlight the correct answer in green
                    highlightCorrectAnswer();
                }

                enableOptionButtons(false);
                nextButton.setVisibility(View.VISIBLE);
            };

            optionA.setOnClickListener(listener);
            optionB.setOnClickListener(listener);
            optionC.setOnClickListener(listener);
            optionD.setOnClickListener(listener);

            nextButton.setOnClickListener(v -> {
                currentIndex++;
                Log.i("QuizActivity", "Current Index: " + currentIndex);
                displayQuestion();
            });

        } else {
            // Quiz completed - set progress to 100%
            if (progressBar != null) {
                progressBar.setProgress(10);
            }
            if (questionCounter != null) {
                questionCounter.setText("10/10");
            }

            prefs.edit().putBoolean("quizInProgress", false).apply();
            clearQuizState();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("QuizCompleted", true).apply();

            questionView.setTextSize(24);
            questionView.setText("Quiz Completed! 🎉");

            TextView correctCountView = new TextView(this);
            correctCountView.setTextSize(18);
            correctCountView.setText("Score: " + correctCount + "/10 (" + (correctCount * 10) + "%)");
            correctCountView.setTextColor(Color.parseColor(PRIMARY_BLUE));
            correctCountView.setPadding(0, 20, 0, 0);
            correctCountView.setGravity(android.view.Gravity.CENTER);
            ((ViewGroup) questionView.getParent()).addView(correctCountView);

            // Add performance message
            TextView performanceView = new TextView(this);
            performanceView.setTextSize(16);
            String performance = getPerformanceMessage(correctCount);
            performanceView.setText(performance);
            performanceView.setTextColor(Color.parseColor("#6B7280"));
            performanceView.setPadding(0, 10, 0, 0);
            performanceView.setGravity(android.view.Gravity.CENTER);
            ((ViewGroup) questionView.getParent()).addView(performanceView);

            nextButton.setVisibility(View.GONE);
        }
    }

    private String getPerformanceMessage(int score) {
        if (score >= 9) return "Excellent! Outstanding performance! 🌟";
        else if (score >= 7) return "Great job! Well done! 👏";
        else if (score >= 5) return "Good effort! Keep practicing! 💪";
        else return "Keep learning! You'll improve! 📚";
    }

    private void resetButtonColors() {
        // Reset all option buttons to blue with white text
        setButtonStyle(optionA, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionB, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionC, PRIMARY_BLUE, Color.WHITE);
        setButtonStyle(optionD, PRIMARY_BLUE, Color.WHITE);
    }

    private void enableOptionButtons(boolean enable) {
        optionA.setEnabled(enable);
        optionB.setEnabled(enable);
        optionC.setEnabled(enable);
        optionD.setEnabled(enable);
    }

    private void highlightCorrectAnswer() {
        int correctIndex = correctAnswers[currentIndex];
        Button correctButton = (correctIndex == 0) ? optionA :
                (correctIndex == 1) ? optionB :
                        (correctIndex == 2) ? optionC : optionD;

        // Highlight correct answer in green
        setButtonStyle(correctButton, CORRECT_GREEN, Color.WHITE);
    }

    private void saveQuizState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("quizInProgress", true);
        editor.putInt("currentIndex", currentIndex);
        editor.putInt("correctCount", correctCount);
        for (int i = 0; i < questions.length; i++) {
            editor.putString("question_" + i, questions[i]);
            for (int j = 0; j < options[i].length; j++) {
                editor.putString("option_" + i + "_" + j, options[i][j]);
            }
            editor.putInt("correctAnswer_" + i, correctAnswers[i]);
        }
        editor.apply();
    }

    private void restoreQuizState() {
        currentIndex = prefs.getInt("currentIndex", 0);
        correctCount = prefs.getInt("correctCount", 0);
        for (int i = 0; i < questions.length; i++) {
            questions[i] = prefs.getString("question_" + i, null);
            for (int j = 0; j < options[i].length; j++) {
                options[i][j] = prefs.getString("option_" + i + "_" + j, null);
            }
            correctAnswers[i] = prefs.getInt("correctAnswer_" + i, -1);
        }
        if (!CheckData()) {
            questionView.setText("Error: Incomplete quiz data.");
            return;
        }
        displayQuestion();
    }

    private void clearQuizState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("quizInProgress");
        editor.remove("currentQuestionIndex");
        editor.remove("correctCount");
        for (int i = 0; i < questions.length; i++) {
            editor.remove("question_" + i);
            for (int j = 0; j < options[i].length; j++) {
                editor.remove("option_" + i + "_" + j);
            }
            editor.remove("correctAnswer_" + i);
        }
        editor.apply();
    }

    private boolean CheckData() {
        for (int i = 0; i < questions.length; i++) {
            if (questions[i] == null || options[i][0] == null) {
                return false;
            }
        }
        return true;
    }
}
