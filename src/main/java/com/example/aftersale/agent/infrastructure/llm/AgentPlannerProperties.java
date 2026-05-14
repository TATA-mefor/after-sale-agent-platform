package com.example.aftersale.agent.infrastructure.llm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.planner")
public class AgentPlannerProperties {

    private AgentPlannerMode mode = AgentPlannerMode.RULE;

    private Llm llm = new Llm();

    public AgentPlannerMode getMode() {
        return mode;
    }

    public void setMode(AgentPlannerMode mode) {
        this.mode = mode;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Spring Boot configuration properties expose nested mutable property objects for binding.")
    public Llm getLlm() {
        return llm;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring Boot configuration binding stores nested mutable property objects.")
    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public static class Llm {

        private String provider = "openai";

        private String model = "gpt-4o-mini";

        private String apiKey = "";

        private int timeoutSeconds = 30;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
