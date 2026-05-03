package com.hhgolf.golf_picks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PlayerSeasonStatId implements Serializable {

    @Column(name = "player_id")
    private String playerId;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "stat_id")
    private String statId;
}
