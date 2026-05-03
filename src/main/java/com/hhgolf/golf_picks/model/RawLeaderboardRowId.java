package com.hhgolf.golf_picks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Data;

@Data
@Embeddable
public class RawLeaderboardRowId implements Serializable {

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "player_id")
    private String playerId;
}
