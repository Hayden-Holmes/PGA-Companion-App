package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "raw_leaderboard_rows")
public class RawLeaderboardRow {

    @EmbeddedId
    private RawLeaderboardRowId id;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "round_1_score")
    private Integer round1Score;

    @Column(name = "round_2_score")
    private Integer round2Score;

    @Column(name = "round_3_score")
    private Integer round3Score;

    @Column(name = "round_4_score")
    private Integer round4Score;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "position")
    private Integer position;

    @Column(name = "status")
    private String status;

    @Column(name = "source_url")
    private String sourceUrl;
}