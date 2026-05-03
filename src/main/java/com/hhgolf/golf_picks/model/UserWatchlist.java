package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_watchlist")
public class UserWatchlist {

    @EmbeddedId
    private UserWatchlistId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    public UserWatchlist(User user, Player player) {
        this.user = user;
        this.player = player;
        this.id = new UserWatchlistId(user.getUserId(), player.getPlayerId());
    }
}
