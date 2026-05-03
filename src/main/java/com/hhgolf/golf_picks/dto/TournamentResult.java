package com.hhgolf.golf_picks.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class TournamentResult {

    private String tournamentId;
    private String tournamentName;
    private Integer seasonYear;
    private LocalDate endDate;

    // From raw_leaderboard_rows
    private Integer position;       // final finishing position, null if CUT/WD
    private String  status;         // "FIN", "CUT", "WD"

    // Scoring — relative to par (calculated in service)
    private Integer totalScore;         // sum of raw round scores (strokes)
    private Integer totalScoreVsPar;    // totalScore - (par * roundsPlayed)
    private List<Integer> roundScores;  // raw strokes per round
    private List<Integer> roundScoresVsPar; // strokes - par per round

    // Averaged per-round stats
    private BigDecimal avgGir;
    private BigDecimal avgFairwaysHit;
    private BigDecimal avgDrivingDistance;
    private BigDecimal avgPutts;
    private BigDecimal avgSgTotal;
    private BigDecimal avgSgOtt;
    private BigDecimal avgSgApp;
    private BigDecimal avgSgArg;
    private BigDecimal avgSgPutt;
    private BigDecimal avgScrambling;

    // Scoring summary
    private Integer totalBirdies;
    private Integer totalBogeys;
    private Integer totalDoubleBogeys;

    private int roundsPlayed;
    private Integer coursePar;  // par for the course, used for vs-par calculation

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getTournamentId()   { return tournamentId; }
    public void setTournamentId(String v) { this.tournamentId = v; }

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String v) { this.tournamentName = v; }

    public Integer getSeasonYear()    { return seasonYear; }
    public void setSeasonYear(Integer v) { this.seasonYear = v; }

    public LocalDate getEndDate()     { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }

    public Integer getPosition()      { return position; }
    public void setPosition(Integer v) { this.position = v; }

    public String getStatus()         { return status; }
    public void setStatus(String v)   { this.status = v; }

    public Integer getTotalScore()    { return totalScore; }
    public void setTotalScore(Integer v) { this.totalScore = v; }

    public Integer getTotalScoreVsPar() { return totalScoreVsPar; }
    public void setTotalScoreVsPar(Integer v) { this.totalScoreVsPar = v; }

    public List<Integer> getRoundScores() { return roundScores; }
    public void setRoundScores(List<Integer> v) { this.roundScores = v; }

    public List<Integer> getRoundScoresVsPar() { return roundScoresVsPar; }
    public void setRoundScoresVsPar(List<Integer> v) { this.roundScoresVsPar = v; }

    public BigDecimal getAvgGir()     { return avgGir; }
    public void setAvgGir(BigDecimal v) { this.avgGir = v; }

    public BigDecimal getAvgFairwaysHit() { return avgFairwaysHit; }
    public void setAvgFairwaysHit(BigDecimal v) { this.avgFairwaysHit = v; }

    public BigDecimal getAvgDrivingDistance() { return avgDrivingDistance; }
    public void setAvgDrivingDistance(BigDecimal v) { this.avgDrivingDistance = v; }

    public BigDecimal getAvgPutts()   { return avgPutts; }
    public void setAvgPutts(BigDecimal v) { this.avgPutts = v; }

    public BigDecimal getAvgSgTotal() { return avgSgTotal; }
    public void setAvgSgTotal(BigDecimal v) { this.avgSgTotal = v; }

    public BigDecimal getAvgSgOtt()   { return avgSgOtt; }
    public void setAvgSgOtt(BigDecimal v) { this.avgSgOtt = v; }

    public BigDecimal getAvgSgApp()   { return avgSgApp; }
    public void setAvgSgApp(BigDecimal v) { this.avgSgApp = v; }

    public BigDecimal getAvgSgArg()   { return avgSgArg; }
    public void setAvgSgArg(BigDecimal v) { this.avgSgArg = v; }

    public BigDecimal getAvgSgPutt()  { return avgSgPutt; }
    public void setAvgSgPutt(BigDecimal v) { this.avgSgPutt = v; }

    public BigDecimal getAvgScrambling() { return avgScrambling; }
    public void setAvgScrambling(BigDecimal v) { this.avgScrambling = v; }

    public Integer getTotalBirdies()  { return totalBirdies; }
    public void setTotalBirdies(Integer v) { this.totalBirdies = v; }

    public Integer getTotalBogeys()   { return totalBogeys; }
    public void setTotalBogeys(Integer v) { this.totalBogeys = v; }

    public Integer getTotalDoubleBogeys() { return totalDoubleBogeys; }
    public void setTotalDoubleBogeys(Integer v) { this.totalDoubleBogeys = v; }

    public int getRoundsPlayed()      { return roundsPlayed; }
    public void setRoundsPlayed(int v) { this.roundsPlayed = v; }

    public Integer getCoursePar()     { return coursePar; }
    public void setCoursePar(Integer v) { this.coursePar = v; }

    // ── Display helpers ──────────────────────────────────────────────────────

    /** Position display: "1", "T4", "CUT", "WD" */
    public String getPositionDisplay() {
        if ("CUT".equals(status)) return "CUT";
        if ("WD".equals(status))  return "WD";
        if (position == null)     return "-";
        return String.valueOf(position);
    }

    /** Score vs par: "-12", "+3", "E", "CUT", "WD" */
    public String getScoreDisplay() {
        if ("CUT".equals(status)) return "CUT";
        if ("WD".equals(status))  return "WD";
        if (totalScoreVsPar == null) {
            // Fall back to raw total if par not available
            if (totalScore == null) return "-";
            if (totalScore == 0)    return "E";
            return totalScore > 0 ? "+" + totalScore : String.valueOf(totalScore);
        }
        if (totalScoreVsPar == 0) return "E";
        return totalScoreVsPar > 0 ? "+" + totalScoreVsPar : String.valueOf(totalScoreVsPar);
    }

    /** Per-round score vs par display: "-3", "+1", "E" */
    public static String roundVsParDisplay(Integer scoreVsPar) {
        if (scoreVsPar == null) return "-";
        if (scoreVsPar == 0)    return "E";
        return scoreVsPar > 0 ? "+" + scoreVsPar : String.valueOf(scoreVsPar);
    }

    /** Format BigDecimal to 2dp, "-" if null */
    public static String fmt(BigDecimal v) {
        if (v == null) return "-";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}