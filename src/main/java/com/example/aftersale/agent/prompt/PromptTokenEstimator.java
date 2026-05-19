package com.example.aftersale.agent.prompt;

public final class PromptTokenEstimator {

    private PromptTokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        return Math.max(1, text.length() / 4);
    }
}
