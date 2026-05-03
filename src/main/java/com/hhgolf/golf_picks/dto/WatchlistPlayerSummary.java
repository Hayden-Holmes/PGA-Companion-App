package com.hhgolf.golf_picks.dto;

import java.math.BigDecimal;

/**
 * Compact summary of a watched player shown on the dashboard.
 */
public class WatchlistPlayerSummary {

    private String playerId;
    private String playerName;
    private String country;

    // Most recent tournament
    private String lastTournamentName;
    private String lastScoreDisplay;   // e.g. "-12", "+3", "E"

    // Trend: positive sgTotal trend = improving
    private BigDecimal sgTotal;        // most recent tournament avg
    private TrendDirection trend;      // UP, DOWN, FLAT

    public enum TrendDirection { UP, DOWN, FLAT }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getPlayerId()           { return playerId; }
    public void setPlayerId(String v)     { this.playerId = v; }

    public String getPlayerName()         { return playerName; }
    public void setPlayerName(String v)   { this.playerName = v; }

    public String getCountry()            { return country; }
    public void setCountry(String v)      { this.country = v; }

    public String getLastTournamentName() { return lastTournamentName; }
    public void setLastTournamentName(String v) { this.lastTournamentName = v; }

    public String getLastScoreDisplay()   { return lastScoreDisplay; }
    public void setLastScoreDisplay(String v) { this.lastScoreDisplay = v; }

    public BigDecimal getSgTotal()        { return sgTotal; }
    public void setSgTotal(BigDecimal v)  { this.sgTotal = v; }

    public TrendDirection getTrend()      { return trend; }
    public void setTrend(TrendDirection v) { this.trend = v; }
}