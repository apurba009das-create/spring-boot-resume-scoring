package com.talentmatch.model;

import java.util.Map;

public class ScoreResult {
    private double score;
    private Map<String, Double> subscores;
    private String explanation;

    public ScoreResult() {}

    public ScoreResult(double score, Map<String, Double> subscores, String explanation) {
        this.score = score;
        this.subscores = subscores;
        this.explanation = explanation;
    }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public Map<String, Double> getSubscores() { return subscores; }
    public void setSubscores(Map<String, Double> subscores) { this.subscores = subscores; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
