package com.example.test;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Vocabulary extends AppCompatActivity {

    private ListView vocabularyListView;
    private EditText searchEditText;
    private TextView levelBadge, totalWordsCount, learnedWordsCount, progressPercentage, resultCount;
    private LinearLayout emptyStateView;
    private Button filterAll, filterNouns, filterVerbs, filterAdjectives, filterPhrases;
    private Button backToHomeButton, practiceButton;

    private ProgressDialog progressDialog;
    private final OkHttpClient client = new OkHttpClient();
    private final List<VocabularyItem> vocabularyList = new ArrayList<>();
    private final List<VocabularyItem> filteredList = new ArrayList<>();
    private VocabularyAdapter adapter;
    private String currentFilter = "All";

    // Color constants
    private static final String PRIMARY_BLUE = "#3B82F6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary);

        initializeViews();
        setupUI();
        setupListeners();

        // Initialize and show the ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading vocabulary...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        fetchVocabularyFromAPI();
    }

    private void initializeViews() {
        vocabularyListView = findViewById(R.id.vocabularyListView);
        searchEditText = findViewById(R.id.searchEditText);
        levelBadge = findViewById(R.id.levelBadge);
        totalWordsCount = findViewById(R.id.totalWordsCount);
        learnedWordsCount = findViewById(R.id.learnedWordsCount);
        progressPercentage = findViewById(R.id.progressPercentage);
        resultCount = findViewById(R.id.resultCount);
        emptyStateView = findViewById(R.id.emptyStateView);

        filterAll = findViewById(R.id.filterAll);
        filterNouns = findViewById(R.id.filterNouns);
        filterVerbs = findViewById(R.id.filterVerbs);
        filterAdjectives = findViewById(R.id.filterAdjectives);
        filterPhrases = findViewById(R.id.filterPhrases);

        backToHomeButton = findViewById(R.id.backToHomeButton);
        practiceButton = findViewById(R.id.practiceButton);
    }

    private void setupUI() {
        // Set level badge
        String level = getIntent().getStringExtra("quizLevel");
        if (level != null) {
            levelBadge.setText(level);
        }

        // Setup adapter
        adapter = new VocabularyAdapter(this, filteredList);
        vocabularyListView.setAdapter(adapter);

        // Set button colors
        setButtonColors();

        // Initially select "All" filter
        selectFilter(filterAll);
    }

    private void setButtonColors() {
        try {
            // Set filter button colors
            setButtonStyle(filterAll, PRIMARY_BLUE, Color.WHITE);
            setButtonStyle(filterNouns, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
            setButtonStyle(filterVerbs, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
            setButtonStyle(filterAdjectives, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
            setButtonStyle(filterPhrases, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));

            // Set action button colors
            setButtonStyle(practiceButton, PRIMARY_BLUE, Color.WHITE);
        } catch (Exception e) {
            Log.e("Vocabulary", "Error setting button colors: " + e.getMessage());
        }
    }

    private void setButtonStyle(Button button, String backgroundColor, int textColor) {
        try {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(backgroundColor))
            );
            button.setTextColor(textColor);
        } catch (Exception e) {
            Log.e("Vocabulary", "Error setting button style: " + e.getMessage());
        }
    }

    private void setupListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVocabulary();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter buttons
        filterAll.setOnClickListener(v -> {
            currentFilter = "All";
            selectFilter(filterAll);
            filterVocabulary();
        });

        filterNouns.setOnClickListener(v -> {
            currentFilter = "Noun";
            selectFilter(filterNouns);
            filterVocabulary();
        });

        filterVerbs.setOnClickListener(v -> {
            currentFilter = "Verb";
            selectFilter(filterVerbs);
            filterVocabulary();
        });

        filterAdjectives.setOnClickListener(v -> {
            currentFilter = "Adjective";
            selectFilter(filterAdjectives);
            filterVocabulary();
        });

        filterPhrases.setOnClickListener(v -> {
            currentFilter = "Phrase";
            selectFilter(filterPhrases);
            filterVocabulary();
        });

        // Action buttons
        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(Vocabulary.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        practiceButton.setOnClickListener(v -> {
            // TODO: Implement practice functionality
            Toast.makeText(this, "Practice feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void selectFilter(Button selectedButton) {
        // Reset all filter buttons
        setButtonStyle(filterAll, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
        setButtonStyle(filterNouns, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
        setButtonStyle(filterVerbs, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
        setButtonStyle(filterAdjectives, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));
        setButtonStyle(filterPhrases, "#DBEAFE", Color.parseColor(PRIMARY_BLUE));

        // Highlight selected button
        setButtonStyle(selectedButton, PRIMARY_BLUE, Color.BLACK);
    }

    private void filterVocabulary() {
        filteredList.clear();
        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();

        for (VocabularyItem item : vocabularyList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    item.getWord().toLowerCase().contains(searchQuery) ||
                    item.getMeaning().toLowerCase().contains(searchQuery);

            boolean matchesFilter = currentFilter.equals("All") ||
                    item.getType().equals(currentFilter);

            if (matchesSearch && matchesFilter) {
                filteredList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        updateResultCount();
        updateEmptyState();
    }

    private void updateResultCount() {
        resultCount.setText(filteredList.size() + " words");
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            vocabularyListView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            vocabularyListView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void updateStats() {
        int totalWords = vocabularyList.size();
        int learnedWords = (int) (totalWords * 0.3); // Simulate 30% learned
        int progress = totalWords > 0 ? (learnedWords * 100) / totalWords : 0;

        totalWordsCount.setText(String.valueOf(totalWords));
        learnedWordsCount.setText(String.valueOf(learnedWords));
        progressPercentage.setText(progress + "%");
    }

    private String getQuizLevel() {
        String level = getIntent().getStringExtra("quizLevel");
        if (level == null) return "Medium";

        switch (level) {
            case "Beginner": return "Easy";
            case "Intermediate": return "Medium";
            case "Advanced": return "Hard";
            default: return "Medium";
        }
    }

    private void fetchVocabularyFromAPI() {
        String quizLevel = getIntent().getStringExtra("quizLevel");
        JSONObject jsonBody = new JSONObject();

        try {
            JSONArray contents = new JSONArray();
            JSONObject contentObject = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject partObject = new JSONObject();

            partObject.put("text", "Generate 20 " + quizLevel.toLowerCase() +
                    " level English words with their meanings and word types (noun, verb, adjective, etc.). " +
                    "Each meaning must be clear and at least 5 words long. Ensure the vocabulary is " +
                    "appropriate for the " + quizLevel + " level. Return as a JSON array of objects with " +
                    "'word', 'meaning', and 'type' keys. Example: " +
                    "[{\"word\":\"example\",\"meaning\":\"a thing characteristic of its kind\",\"type\":\"noun\"}]");

            parts.put(partObject);
            contentObject.put("parts", parts);
            contents.put(contentObject);
            jsonBody.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
            return;
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
                    progressDialog.dismiss();
                    Toast.makeText(Vocabulary.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Vocabulary", "API call failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(Vocabulary.this, "API Error: " + response.message(), Toast.LENGTH_LONG).show();
                        Log.e("Vocabulary", "API response unsuccessful: " + response.message());
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d("Vocabulary", "Response Body: " + responseBody);

                    JSONObject body = new JSONObject(responseBody);
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
                    JSONArray array = new JSONArray(text);

                    vocabularyList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String word = obj.getString("word");
                        String meaning = obj.getString("meaning");
                        String type = obj.optString("type", "Word");

                        // Capitalize first letter of type
                        type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                        vocabularyList.add(new VocabularyItem(word, meaning, type));
                    }

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        filterVocabulary(); // This will update the filtered list and adapter
                        updateStats();
                        Toast.makeText(Vocabulary.this, "Vocabulary loaded successfully!", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e("Vocabulary", "Parsing error: " + e.getMessage());
                        Toast.makeText(Vocabulary.this, "Error parsing vocabulary data", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    // VocabularyItem class to hold word data
    public static class VocabularyItem {
        private String word;
        private String meaning;
        private String type;

        public VocabularyItem(String word, String meaning, String type) {
            this.word = word;
            this.meaning = meaning;
            this.type = type;
        }

        public String getWord() { return word; }
        public String getMeaning() { return meaning; }
        public String getType() { return type; }
    }

    // Custom adapter for better display
    private static class VocabularyAdapter extends ArrayAdapter<VocabularyItem> {
        public VocabularyAdapter(android.content.Context context, List<VocabularyItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            VocabularyItem item = getItem(position);
            if (item != null) {
                TextView text1 = convertView.findViewById(android.R.id.text1);
                TextView text2 = convertView.findViewById(android.R.id.text2);

                text1.setText(item.getWord() + " (" + item.getType() + ")");
                text1.setTextColor(Color.parseColor("#1F2937"));
                text1.setTextSize(16);
                text1.setTypeface(null, android.graphics.Typeface.BOLD);

                text2.setText(item.getMeaning());
                text2.setTextColor(Color.parseColor("#6B7280"));
                text2.setTextSize(14);
            }

            return convertView;
        }
    }
}
