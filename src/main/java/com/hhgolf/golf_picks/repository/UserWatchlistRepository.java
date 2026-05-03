package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.Player;
import com.hhgolf.golf_picks.model.User;
import com.hhgolf.golf_picks.model.UserWatchlist;
import com.hhgolf.golf_picks.model.UserWatchlistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserWatchlistRepository extends JpaRepository<UserWatchlist, UserWatchlistId> {

    @Query("SELECT w.player FROM UserWatchlist w WHERE w.user = :user ORDER BY w.player.playerName ASC")
    List<Player> findPlayersByUser(@Param("user") User user);

    boolean existsByUserAndPlayer(User user, Player player);

    void deleteByUserAndPlayer(User user, Player player);
}