package com.htz.ai;

/**
 * LLM providers supported by {@link AiClient}, resolved from the model name prefix.
 */
public enum AiProvider {

    OPENAI("gpt"),
    GEMINI("gemini"),
    CLAUDE("claude");

    private final String modelPrefix;

    AiProvider(String modelPrefix) {
        this.modelPrefix = modelPrefix;
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
