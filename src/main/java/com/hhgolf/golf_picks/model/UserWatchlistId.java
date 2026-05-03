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
public class UserWatchlistId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "player_id")
    private String playerId;
}
