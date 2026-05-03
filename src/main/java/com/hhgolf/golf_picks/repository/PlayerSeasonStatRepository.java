

// ─── PlayerSeasonStatRepository.java ────────────────────────────────────────
package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.PlayerSeasonStat;
import com.hhgolf.golf_picks.model.PlayerSeasonStatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerSeasonStatRepository extends JpaRepository<PlayerSeasonStat, PlayerSeasonStatId> {

    @Query("""
        SELECT s FROM PlayerSeasonStat s
        WHERE s.player.playerId = :playerId
          AND s.id.seasonYear = :year
        ORDER BY s.statTitle ASC
    """)
    List<PlayerSeasonStat> findByPlayerIdAndYear(
            @Param("playerId") String playerId,
            @Param("year") Integer year);

    @Query("SELECT MAX(s.id.seasonYear) FROM PlayerSeasonStat s WHERE s.player.playerId = :playerId")
    Integer findLatestSeasonYear(@Param("playerId") String playerId);

    @Query("SELECT DISTINCT s.id.statId FROM PlayerSeasonStat s ORDER BY s.id.statId ASC")
    List<String> findAllStatIds();
}