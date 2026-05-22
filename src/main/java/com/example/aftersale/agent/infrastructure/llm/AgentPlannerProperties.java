package com.example.aftersale.agent.infrastructure.llm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 承载 Agent Planner 的外部化配置。
 *
 * <p>边界：默认配置必须指向离线规则模式；真实 Provider 凭证只能通过运行环境注入，不能写入代码或测试数据。
 */
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

        private String provider = "openai-responses";

        private String model = "gpt-4.1-mini";

        private String apiKey = "";

        private String endpoint = "https://api.openai.com/v1/responses";

        private int timeoutSeconds = 30;

        private Budget budget = new Budget();

        private DashScope dashscope = new DashScope();

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

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "Spring Boot configuration properties expose nested mutable property objects.")
        public Budget getBudget() {
            return budget;
        }

        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP2",
                justification = "Spring Boot configuration binding stores nested mutable property objects.")
        public void setBudget(Budget budget) {
            this.budget = budget;
        }

        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "Spring Boot configuration properties expose nested mutable property objects.")
        public DashScope getDashscope() {
            return dashscope;
        }

        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP2",
                justification = "Spring Boot configuration binding stores nested mutable property objects.")
        public void setDashscope(DashScope dashscope) {
            this.dashscope = dashscope;
        }
    }

    public static class DashScope {

        private String apiKey = "";

        private String baseUrl = "https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1";

        private String responsesEndpoint = "";

        private String chatCompletionsEndpoint = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getResponsesEndpoint() {
            return responsesEndpoint;
        }

        public void setResponsesEndpoint(String responsesEndpoint) {
            this.responsesEndpoint = responsesEndpoint;
        }

        public String getChatCompletionsEndpoint() {
            return chatCompletionsEndpoint;
        }

        public void setChatCompletionsEndpoint(String chatCompletionsEndpoint) {
            this.chatCompletionsEndpoint = chatCompletionsEndpoint;
        }
    }

    public static class Budget {

        private int systemPromptTokens = 2000;

        private int historyTokens = 4000;

        private int ragContextTokens = 8000;

        private int toolCatalogTokens = 2000;

        private int maxOutputTokens = 1000;

        private int totalInputTokens = 16000;

        public int getSystemPromptTokens() {
            return systemPromptTokens;
        }

        public void setSystemPromptTokens(int systemPromptTokens) {
            this.systemPromptTokens = systemPromptTokens;
        }

        public int getHistoryTokens() {
            return historyTokens;
        }

        public void setHistoryTokens(int historyTokens) {
            this.historyTokens = historyTokens;
        }

        public int getRagContextTokens() {
            return ragContextTokens;
        }

        public void setRagContextTokens(int ragContextTokens) {
            this.ragContextTokens = ragContextTokens;
        }

        public int getToolCatalogTokens() {
            return toolCatalogTokens;
        }

        public void setToolCatalogTokens(int toolCatalogTokens) {
            this.toolCatalogTokens = toolCatalogTokens;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public int getTotalInputTokens() {
            return totalInputTokens;
        }

        public void setTotalInputTokens(int totalInputTokens) {
            this.totalInputTokens = totalInputTokens;
        }
    }
}
