package com.hhgolf.golf_picks.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents how well a single player fits a specific course.
 * Built by CourseService and rendered on the course-fit page.
 */
public class CourseFitEntry {

    private String playerId;
    private String playerName;
    private String country;

    // Composite fit score (0–10 scale, higher = better fit)
    private double fitScore;

    // Letter grade for display
    private String fitGrade;

    // The primary driver of the fit score (e.g. "Strong off-the-tee")
    private String fitSummary;

    // Weighted SG components (from recent rounds, adjusted per course type)
    private BigDecimal weightedSgOtt;
    private BigDecimal weightedSgApp;
    private BigDecimal weightedSgArg;
    private BigDecimal weightedSgPutt;

    // Raw recent averages for display
    private BigDecimal avgSgOtt;
    private BigDecimal avgSgApp;
    private BigDecimal avgSgArg;
    private BigDecimal avgSgPutt;
    private BigDecimal avgSgTotal;

    // Course history at this specific course
    private int courseAppearances;   // number of tournaments at this course
    private Double avgScoreVsPar;    // avg total score vs par at this course (null if no history)
    private Integer bestFinish;      // best position at this course

    // Mini-history for sparkline (score vs par, most-recent last)
    private List<Integer> courseScoreHistory;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getPlayerId()   { return playerId; }
    public void setPlayerId(String v) { playerId = v; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { playerName = v; }

    public String getCountry()    { return country; }
    public void setCountry(String v) { country = v; }

    public double getFitScore()   { return fitScore; }
    public void setFitScore(double v) { fitScore = v; }

    public String getFitGrade()   { return fitGrade; }
    public void setFitGrade(String v) { fitGrade = v; }

    public String getFitSummary() { return fitSummary; }
    public void setFitSummary(String v) { fitSummary = v; }

    public BigDecimal getWeightedSgOtt()  { return weightedSgOtt; }
    public void setWeightedSgOtt(BigDecimal v) { weightedSgOtt = v; }

    public BigDecimal getWeightedSgApp()  { return weightedSgApp; }
    public void setWeightedSgApp(BigDecimal v) { weightedSgApp = v; }

    public BigDecimal getWeightedSgArg()  { return weightedSgArg; }
    public void setWeightedSgArg(BigDecimal v) { weightedSgArg = v; }

    public BigDecimal getWeightedSgPutt() { return weightedSgPutt; }
    public void setWeightedSgPutt(BigDecimal v) { weightedSgPutt = v; }

    public BigDecimal getAvgSgOtt()   { return avgSgOtt; }
    public void setAvgSgOtt(BigDecimal v) { avgSgOtt = v; }

    public BigDecimal getAvgSgApp()   { return avgSgApp; }
    public void setAvgSgApp(BigDecimal v) { avgSgApp = v; }

    public BigDecimal getAvgSgArg()   { return avgSgArg; }
    public void setAvgSgArg(BigDecimal v) { avgSgArg = v; }

    public BigDecimal getAvgSgPutt()  { return avgSgPutt; }
    public void setAvgSgPutt(BigDecimal v) { avgSgPutt = v; }

    public BigDecimal getAvgSgTotal() { return avgSgTotal; }
    public void setAvgSgTotal(BigDecimal v) { avgSgTotal = v; }

    public int getCourseAppearances() { return courseAppearances; }
    public void setCourseAppearances(int v) { courseAppearances = v; }

    public Double getAvgScoreVsPar()  { return avgScoreVsPar; }
    public void setAvgScoreVsPar(Double v) { avgScoreVsPar = v; }

    public Integer getBestFinish()    { return bestFinish; }
    public void setBestFinish(Integer v) { bestFinish = v; }

    public List<Integer> getCourseScoreHistory() { return courseScoreHistory; }
    public void setCourseScoreHistory(List<Integer> v) { courseScoreHistory = v; }

    // ── Display helpers ──────────────────────────────────────────────────────

    public String getFitScoreDisplay() {
        return String.format("%.1f", fitScore);
    }

    public String getAvgScoreVsParDisplay() {
        if (avgScoreVsPar == null) return "—";
        if (avgScoreVsPar == 0) return "E";
        return avgScoreVsPar > 0
                ? String.format("+%.1f", avgScoreVsPar)
                : String.format("%.1f", avgScoreVsPar);
    }

    public String getBestFinishDisplay() {
        return bestFinish != null ? "T" + bestFinish : "—";
    }

    /** CSS colour class for the fit grade badge */
    public String getFitGradeClass() {
        if (fitGrade == null) return "";
        return switch (fitGrade) {
            case "A+", "A"  -> "grade-a";
            case "B+", "B"  -> "grade-b";
            case "C+", "C"  -> "grade-c";
            default          -> "grade-d";
        };
    }
}
