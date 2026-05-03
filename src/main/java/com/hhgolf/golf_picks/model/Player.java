package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "players")
public class Player {

    @Id
    @Column(name = "player_id")
    private String playerId;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "country")
    private String country;

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<Round> rounds;

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<PlayerSeasonStat> seasonStats;
}
