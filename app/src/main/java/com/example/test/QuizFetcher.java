package com.example.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.*;
import org.json.*;

import java.io.IOException;

public class QuizFetcher {
    public interface QuizFetchCallback {
        void onQuizReady();
        void onQuizError(String errorMsg);
    }

    public static void fetchQuiz(Context context, SharedPreferences prefs, QuizFetchCallback callback) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "deepseek/deepseek-r1:free");
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "Generate a 10-question language quiz with 4 options each (MCQs or fill-in-the-blanks). Format as JSON with 'question', 'options', and 'answerIndex'");
            messages.put(userMessage);
            jsonBody.put("messages", messages);
        } catch (JSONException e) {
            callback.onQuizError("JSON Error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer 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")
                .addHeader("HTTP-Referer", "https://www.sitename.com")
                .addHeader("X-Title", "SiteName")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onQuizError("Network Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onQuizError("API Error: " + response.message());
                    return;
                }

                try {
                    String responseBodyString = response.body().string();
                    JSONObject body = new JSONObject(responseBodyString);
                    JSONArray choices = body.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) throw new JSONException("Missing 'choices'");

                    String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    content = content.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "");

                    JSONArray quizArray;
                    if (content.trim().startsWith("[")) {
                        quizArray = new JSONArray(content);
                    } else {
                        JSONObject quizResponse = new JSONObject(content);
                        quizArray = quizResponse.optJSONArray("quiz");
                        if (quizArray == null) throw new JSONException("Missing 'quiz' array");
                    }

                    if (quizArray.length() != 10) throw new JSONException("Invalid quiz format");

                    SharedPreferences.Editor editor = prefs.edit();
                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject q = quizArray.getJSONObject(i);
                        editor.putString("question_" + i, q.getString("question"));
                        JSONArray opt = q.getJSONArray("options");
                        for (int j = 0; j < 4; j++) {
                            editor.putString("option_" + i + "_" + j, opt.getString(j));
                        }
                        editor.putInt("correctAnswer_" + i, q.getInt("answerIndex"));
                    }
                    editor.putBoolean("quizInProgress", true);
                    editor.putBoolean("QuizCompleted", false);
                    editor.apply();

                    callback.onQuizReady();

                } catch (JSONException e) {
                    callback.onQuizError("Parse Error: " + e.getMessage());
                }
            }
        });
    }
}
