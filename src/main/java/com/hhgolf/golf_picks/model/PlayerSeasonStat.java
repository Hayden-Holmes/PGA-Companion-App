package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "player_season_stats")
public class PlayerSeasonStat {

    @EmbeddedId
    private PlayerSeasonStatId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "stat_name")
    private String statName;

    @Column(name = "stat_value")
    private String statValue;

    @Column(name = "stat_title")
    private String statTitle;

    @Column(name = "tour_avg")
    private String tourAvg;

    @Column(name = "rank")
    private Integer rank;
}
