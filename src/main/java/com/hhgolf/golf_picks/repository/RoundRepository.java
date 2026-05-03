package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, String> {

    @Query("""
        SELECT r FROM Round r
        JOIN FETCH r.tournament t
        JOIN FETCH t.course
        WHERE r.player.playerId = :playerId
        ORDER BY t.endDate DESC, r.roundNumber ASC
    """)
    List<Round> findByPlayerIdWithTournament(@Param("playerId") String playerId);

    /**
     * Fetches recent rounds for watchlist summary.
     * We limit to 8 in Java — enough to cover 2 full tournaments.
     */
  @Query("""
        SELECT r FROM Round r
        JOIN FETCH r.tournament t
        JOIN FETCH t.course
        WHERE r.player.playerId = :playerId
        ORDER BY t.endDate DESC, r.roundNumber ASC
    """)
    List<Round> findRecentRounds(@Param("playerId") String playerId);
}