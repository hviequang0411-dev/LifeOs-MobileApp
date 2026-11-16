package com.lifeos.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lifeos.ai")
public class AiProperties {
    private boolean enabled;
    private boolean budgetAvailable = true;
    private String policyVersion = "2026-11-10";
    private String providerNoticeVersion = "2026-11-10";
    private int dailyRequestLimit = 20;
    private int monthlyRequestLimit = 300;
    private long dailyInputTokenLimit = 100_000;
    private long dailyOutputTokenLimit = 10_000;
    private long monthlyInputTokenLimit = 1_000_000;
    private long monthlyOutputTokenLimit = 100_000;
    private int rateLimitPerMinute = 5;
    private int maxConcurrentPerUser = 2;
    private int maxConcurrentGlobal = 10;
    private String provider = "openai";
    private String model = "gpt-6.1-sol";
    private String openaiApiKey;
    private final Features features = new Features();

    public static class Features {
        private boolean chat;
        private boolean insights;
        private boolean notes;
        private boolean finance;
        private boolean rag;
        private boolean actions;
        public boolean isChat() { return chat; }
        public void setChat(boolean value) { chat = value; }
        public boolean isInsights() { return insights; }
        public void setInsights(boolean value) { insights = value; }
        public boolean isNotes() { return notes; }
        public void setNotes(boolean value) { notes = value; }
        public boolean isFinance() { return finance; }
        public void setFinance(boolean value) { finance = value; }
        public boolean isRag() { return rag; }
        public void setRag(boolean value) { rag = value; }
        public boolean isActions() { return actions; }
        public void setActions(boolean value) { actions = value; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isBudgetAvailable() { return budgetAvailable; }
    public void setBudgetAvailable(boolean value) { budgetAvailable = value; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String value) { policyVersion = value; }
    public String getProviderNoticeVersion() { return providerNoticeVersion; }
    public void setProviderNoticeVersion(String value) { providerNoticeVersion = value; }
    public int getDailyRequestLimit() { return dailyRequestLimit; }
    public void setDailyRequestLimit(int value) { dailyRequestLimit = value; }
    public int getMonthlyRequestLimit() { return monthlyRequestLimit; }
    public void setMonthlyRequestLimit(int value) { monthlyRequestLimit = value; }
    public long getDailyInputTokenLimit() { return dailyInputTokenLimit; }
    public void setDailyInputTokenLimit(long value) { dailyInputTokenLimit = value; }
    public long getDailyOutputTokenLimit() { return dailyOutputTokenLimit; }
    public void setDailyOutputTokenLimit(long value) { dailyOutputTokenLimit = value; }
    public long getMonthlyInputTokenLimit() { return monthlyInputTokenLimit; }
    public void setMonthlyInputTokenLimit(long value) { monthlyInputTokenLimit = value; }
    public long getMonthlyOutputTokenLimit() { return monthlyOutputTokenLimit; }
    public void setMonthlyOutputTokenLimit(long value) { monthlyOutputTokenLimit = value; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int value) { rateLimitPerMinute = value; }
    public int getMaxConcurrentPerUser() { return maxConcurrentPerUser; }
    public void setMaxConcurrentPerUser(int value) { maxConcurrentPerUser = value; }
    public int getMaxConcurrentGlobal() { return maxConcurrentGlobal; }
    public void setMaxConcurrentGlobal(int value) { maxConcurrentGlobal = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getModel() { return model; }
    public void setModel(String value) { model = value; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String value) { openaiApiKey = value; }
    public Features getFeatures() { return features; }
}
