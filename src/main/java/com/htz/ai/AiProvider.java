package com.htz.ai;

/**
 * LLM providers supported by {@link AiClient}, resolved from the model name prefix.
 */
public enum AiProvider {

    OPENAI("gpt", "OPENAI_API_KEY"),
    GROK("grok", "GROK_API_KEY"),
    GEMINI("gemini", "GOOGLE_API_KEY"),
    CLAUDE("claude", "ANTHROPIC_API_KEY");

    private final String modelPrefix;
    private final String apiKeyName;

    AiProvider(String modelPrefix, String apiKeyName) {
        this.modelPrefix = modelPrefix;
        this.apiKeyName = apiKeyName;
    }

    /**
     * @return the API key from the system property or environment variable named {@link #apiKeyName}
     * @throws IllegalStateException when neither is set
     */
    public String apiKey() {
        String key = System.getProperty(apiKeyName);
        if (key == null || key.isBlank()) {
            key = System.getenv(apiKeyName);
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("no API key: set the " + apiKeyName + " system property or environment variable");
        }
        return key;
    }

    /**
     * @return the provider serving {@code model}
     * @throws IllegalArgumentException when no provider matches
     */
    public static AiProvider fromModel(String model) {
        if (model != null) {
            for (AiProvider provider : values()) {
                if (model.startsWith(provider.modelPrefix)) {
                    return provider;
                }
            }
        }
        throw new IllegalArgumentException("model not supported: " + model);
    }
}
