package com.example.myapplication.network;

import androidx.annotation.NonNull;

import com.example.myapplication.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class GroqApiClient {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama3-8b-8192";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build();

    private GroqApiClient() {
    }

    public interface GroqCallback {
        void onSuccess(double score, String feedback, String strengths, String improvements, String verdict);

        void onError(String message);
    }

    public static void evaluateAnswer(
        String question,
        String userAnswer,
        String topic,
        String difficulty,
        GroqCallback callback
    ) {
        if (callback == null) {
            return;
        }

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Missing Groq API key");
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("model", MODEL);
            payload.put("messages", buildMessages(question, userAnswer, topic, difficulty));
            payload.put("max_tokens", 300);
            payload.put("temperature", 0.3);
        } catch (JSONException e) {
            callback.onError("Failed to build Groq request");
            return;
        }

        RequestBody requestBody = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                String body = null;
                try {
                    if (!response.isSuccessful()) {
                        callback.onError("Groq API error: HTTP " + response.code());
                        return;
                    }

                    if (response.body() == null) {
                        callback.onError("Groq API returned an empty response");
                        return;
                    }

                    body = response.body().string();
                    ParsedEvaluation parsed = parseEvaluation(body);
                    callback.onSuccess(parsed.score, parsed.feedback, parsed.strengths, parsed.improvements, parsed.verdict);
                } catch (Exception e) {
                    callback.onError("Failed to parse Groq response");
                } finally {
                    response.close();
                }
            }
        });
    }

    private static JSONArray buildMessages(String question, String userAnswer, String topic, String difficulty) throws JSONException {
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put(
            "content",
            "You are a technical interview evaluator. Evaluate the candidate's answer and respond in exactly this JSON format: {\"score\": <0-100>, \"feedback\": \"<2-3 sentence evaluation>\", \"strengths\": \"<what they did well>\", \"improvements\": \"<what to improve>\", \"verdict\": \"<Excellent/Good/Fair/Poor>\"}"
        );

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put(
            "content",
            "Question: " + safe(question) + "\n"
                + "Topic: " + safe(topic) + "\n"
                + "Difficulty: " + safe(difficulty) + "\n"
                + "Candidate Answer: " + safe(userAnswer) + "\n\n"
                + "Evaluate this answer."
        );

        messages.put(system);
        messages.put(user);
        return messages;
    }

    private static ParsedEvaluation parseEvaluation(String responseBody) throws JSONException {
        JSONObject responseJson = new JSONObject(responseBody);
        JSONArray choices = responseJson.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new JSONException("Missing choices");
        }

        JSONObject choice0 = choices.getJSONObject(0);
        JSONObject message = choice0.getJSONObject("message");
        String content = message.optString("content", "");
        if (content.trim().isEmpty()) {
            throw new JSONException("Missing content");
        }

        JSONObject evaluation = new JSONObject(extractJsonObject(content));

        ParsedEvaluation parsed = new ParsedEvaluation();
        parsed.score = clampScore(evaluation.optDouble("score", 0));
        parsed.feedback = fallback(evaluation.optString("feedback", ""), "Feedback unavailable.");
        parsed.strengths = fallback(evaluation.optString("strengths", ""), "No strengths extracted.");
        parsed.improvements = fallback(evaluation.optString("improvements", ""), "No improvement notes extracted.");
        parsed.verdict = normalizeVerdict(evaluation.optString("verdict", ""), parsed.score);
        return parsed;
    }

    private static String extractJsonObject(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```") && cleaned.contains("{")) {
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return cleaned.substring(firstBrace, lastBrace + 1);
            }
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private static double clampScore(double score) {
        if (score < 0) {
            return 0;
        }
        if (score > 100) {
            return 100;
        }
        return score;
    }

    private static String normalizeVerdict(String verdict, double score) {
        String candidate = verdict == null ? "" : verdict.trim();
        if (
            "Excellent".equalsIgnoreCase(candidate)
                || "Good".equalsIgnoreCase(candidate)
                || "Fair".equalsIgnoreCase(candidate)
                || "Poor".equalsIgnoreCase(candidate)
        ) {
            return capitalize(candidate);
        }

        if (score >= 85) {
            return "Excellent";
        }
        if (score >= 65) {
            return "Good";
        }
        if (score >= 45) {
            return "Fair";
        }
        return "Poor";
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private static String fallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ParsedEvaluation {
        double score;
        String feedback;
        String strengths;
        String improvements;
        String verdict;
    }
}
