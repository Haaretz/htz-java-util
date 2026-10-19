package com.htz.ai;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls to OpenAI / Grok / Gemini / Claude chat APIs, retried with exponential backoff.
 * <p>
 * The response text is returned as-is. On failure the raw response body is returned instead, with
 * {@code - "httpError":<status>} appended for non-200 responses and 200 responses with an {@code error} body,
 * or {@code - "aiError":"no text in response"} when a 200 response holds no text (e.g. a refusal or safety block)
 * - use {@link #isError(String)} to detect it.
 * Plain Java on purpose. The API key is passed in, or read from the provider's system property / environment
 * variable ({@code OPENAI_API_KEY}, {@code GROK_API_KEY}, {@code GOOGLE_API_KEY}, {@code ANTHROPIC_API_KEY}) by the overloads without it.
 */
public class AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiClient.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 1000;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private AiClient() {
    }

    /**
     * {@link #callAi(String, String, Number, String, int)} with {@link #DEFAULT_MAX_ATTEMPTS}.
     */
    public static String callAi(String model, String prompt, Number temperature, String data)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * {@link #callAi(String, String, Number, String, String, int)} with the API key from the provider's system
     * property or environment variable (see {@link AiProvider#apiKey()}).
     *
     * @throws IllegalStateException when the key isn't set
     */
    public static String callAi(String model, String prompt, Number temperature, String data, int maxAttempts)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, AiProvider.fromModel(model).apiKey(), maxAttempts);
    }

    /**
     * {@link #callAi(String, String, Number, String, String, int)} with {@link #DEFAULT_MAX_ATTEMPTS}.
     */
    public static String callAi(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, apiToken, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Calls the model, retrying on error results ({@link #isError(String)}) and I/O failures, sleeping 1s, 2s, 4s...
     * between attempts.
     *
     * @param model       model name, the provider is picked by its prefix (see {@link AiProvider})
     * @param prompt      system prompt / instructions
     * @param temperature optional, {@code null} for the provider default
     * @param data        optional user content, {@code null} to send the prompt only
     * @param apiToken    API key of the model's provider (for OpenAI / Grok a {@code Bearer } prefix is optional)
     * @param maxAttempts total attempts, at least 1
     * @return the last result - check it with {@link #isError(String)}
     * @throws IOException when the last attempt failed with an I/O error
     */
    public static String callAi(String model, String prompt, Number temperature, String data, String apiToken,
                                int maxAttempts) throws IOException, InterruptedException {

        AiProvider provider = AiProvider.fromModel(model);
        long backoff = INITIAL_BACKOFF_MILLIS;
        for (int attempt = 1; ; attempt++) {
            String result;
            try {
                result = callOnce(provider, model, prompt, temperature, data, apiToken);
                if (!isError(result) || attempt >= maxAttempts) {
                    return result;
                }
            } catch (IOException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                result = e.toString();
            }
            LOGGER.warn("AI Call retried - {} - attempt {} - {}", model, attempt, result);
            Thread.sleep(backoff);
            backoff *= 2;
        }
    }

    private static String callOnce(AiProvider provider, String model, String prompt, Number temperature, String data,
                                   String apiToken) throws IOException, InterruptedException {

        switch (provider) {
            case OPENAI:
                return callChatCompletions("https://api.openai.com/v1/chat/completions",
                        model, prompt, temperature, data, apiToken);
            case GROK:
                return callChatCompletions("https://api.x.ai/v1/chat/completions",
                        model, prompt, temperature, data, apiToken);
            case GEMINI:
                return callGemini(model, prompt, temperature, data, apiToken);
            case CLAUDE:
                return callClaude(model, prompt, temperature, data, apiToken);
            default:
                throw new IllegalArgumentException("model not supported: " + model);
        }
    }

    public static boolean isError(String result) {
        return result.contains("\"error\":") || result.contains("\"httpError\":") || result.contains("\"aiError\":");
    }

    /** OpenAI chat completions format, also served by xAI (Grok). */
    private static String callChatCompletions(String url, String model, String prompt, Number temperature, String data,
                                              String apiToken) throws IOException, InterruptedException {

        JSONObject json = new JSONObject();
        json.put("model", model);
        JSONArray messages = new JSONArray();
        JSONObject message;
        message = new JSONObject();
        message.put("role", "system");
        message.put("content", prompt);
        messages.put(message);

        if (data != null) {
            message = new JSONObject();
            message.put("role", "user");
            message.put("content", data);
            messages.put(message);
        }

        json.put("messages", messages);

        if (temperature != null) {
            json.put("temperature", temperature);
        }

        HttpRequest request = jsonPost(url, json)
                .header("Authorization", apiToken.startsWith("Bearer ") ? apiToken : "Bearer " + apiToken)
                .build();

        return send(request, res -> res.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content", null));
    }

    private static String callGemini(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key="
                + URLEncoder.encode(apiToken, StandardCharsets.UTF_8);

        JSONObject message = new JSONObject();
        String userText = prompt;
        if (data != null) {
            message.put("systemInstruction", new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            userText = data;
        }

        JSONObject content = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", userText)));
        message.put("contents", new JSONArray().put(content));

        if (temperature != null) {
            message.put("generationConfig", new JSONObject().put("temperature", temperature));
        }

        return send(jsonPost(url, message).build(), res -> res.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text", null));
    }

    private static String callClaude(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        JSONObject postData = new JSONObject()
                .put("model", model)
                .put("max_tokens", 8192);

        String userText = prompt;
        if (data != null) {
            postData.put("system", prompt);
            userText = data;
        }

        postData.put("messages", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("content", userText)));

        if (temperature != null) {
            postData.put("temperature", temperature);
        }

        HttpRequest request = jsonPost("https://api.anthropic.com/v1/messages", postData)
                .header("anthropic-version", "2023-06-01")
                .header("x-api-key", apiToken)
                .build();

        return send(request, AiClient::claudeText);
    }

    /** Joins the text blocks, skipping thinking / tool_use blocks; {@code null} when there are none. */
    private static String claudeText(JSONObject res) {
        StringBuilder text = null;
        JSONArray content = res.getJSONArray("content");
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.getJSONObject(i);
            if ("text".equals(block.optString("type"))) {
                text = text == null ? new StringBuilder() : text;
                text.append(block.getString("text"));
            }
        }
        return text == null ? null : text.toString();
    }

    private static HttpRequest.Builder jsonPost(String url, JSONObject body) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
    }

    /**
     * Sends the request and returns the text found by {@code extractText}, or the raw body with an error marker
     * when the status isn't 200, the body isn't JSON or {@code extractText} finds no text (returns {@code null}).
     */
    private static String send(HttpRequest request, Function<JSONObject, String> extractText)
            throws IOException, InterruptedException {

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body();
        if (response.statusCode() != 200) {
            return body + " - \"httpError\":" + response.statusCode();
        }

        try {
            JSONObject res = new JSONObject(body);
            if (res.has("error")) {
                // some providers report failures (e.g. overloaded) with a 200 status
                return body + " - \"httpError\":" + response.statusCode();
            }
            String text = extractText.apply(res);
            if (text != null) {
                return text;
            }
        } catch (JSONException e) {
            // no text where expected, fall through
        }
        return body + " - \"aiError\":\"no text in response\"";
    }
}
