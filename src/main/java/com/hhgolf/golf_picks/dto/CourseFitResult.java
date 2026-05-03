package com.hhgolf.golf_picks.dto;

import java.util.List;

/**
 * Wraps the ranked fit entries together with the algorithm metadata used
 * to compute them, so the controller can expose both to the template.
 */
public class CourseFitResult {

    private final List<CourseFitEntry> entries;
    private final double weightOtt;
    private final double weightApp;
    private final double weightArg;
    private final double weightPutt;
    private final String algorithmUsed;
    private final String modelConfidence;
    private final int    dataPoints;
    private final Double rSquared;

    public CourseFitResult(List<CourseFitEntry> entries,
                           double[] weights,
                           String algorithmUsed,
                           String modelConfidence,
                           int dataPoints,
                           Double rSquared) {
        this.entries        = entries;
        this.weightOtt      = weights[0];
        this.weightApp      = weights[1];
        this.weightArg      = weights[2];
        this.weightPutt     = weights[3];
        this.algorithmUsed  = algorithmUsed;
        this.modelConfidence = modelConfidence;
        this.dataPoints     = dataPoints;
        this.rSquared       = rSquared;
    }

    public List<CourseFitEntry> getEntries()        { return entries; }
    public double getWeightOtt()                    { return weightOtt; }
    public double getWeightApp()                    { return weightApp; }
    public double getWeightArg()                    { return weightArg; }
    public double getWeightPutt()                   { return weightPutt; }
    public String getAlgorithmUsed()                { return algorithmUsed; }
    public String getModelConfidence()              { return modelConfidence; }
    public int    getDataPoints()                   { return dataPoints; }
    public Double getRSquared()                     { return rSquared; }

    public boolean isMLPowered() {
        return "ridge_regression".equals(algorithmUsed);
    }

    /** Percent-formatted weight strings for the template. */
    public String getWeightOttPct()  { return Math.round(weightOtt  * 100) + "%"; }
    public String getWeightAppPct()  { return Math.round(weightApp  * 100) + "%"; }
    public String getWeightArgPct()  { return Math.round(weightArg  * 100) + "%"; }
    public String getWeightPuttPct() { return Math.round(weightPutt * 100) + "%"; }

    public String getRSquaredDisplay() {
        if (rSquared == null) return "—";
        return String.format("%.3f", rSquared);
    }

    public String getConfidenceClass() {
        if (modelConfidence == null) return "conf-low";
        return switch (modelConfidence) {
            case "high"   -> "conf-high";
            case "medium" -> "conf-medium";
            default       -> "conf-low";
        };
    }

    public String getAlgorithmLabel() {
        if ("ridge_regression".equals(algorithmUsed)) {
            return "ML · Ridge Regression";
        }
        return "Heuristic · Yardage-Based";
    }
}
