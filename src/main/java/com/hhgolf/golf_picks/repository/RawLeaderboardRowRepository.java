package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.RawLeaderboardRow;
import com.hhgolf.golf_picks.model.RawLeaderboardRowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface RawLeaderboardRowRepository extends JpaRepository<RawLeaderboardRow, RawLeaderboardRowId> {

    @Query("""
        SELECT r FROM RawLeaderboardRow r
        WHERE r.id.eventId = :eventId
          AND r.id.playerId = :playerId
    """)
    Optional<RawLeaderboardRow> findByEventIdAndPlayerId(
            @Param("eventId") String eventId,
            @Param("playerId") String playerId);

    @Query("""
        SELECT r FROM RawLeaderboardRow r
        WHERE r.id.eventId = :eventId
          AND r.position IS NOT NULL
          AND r.position <= 10
        ORDER BY r.position ASC
    """)
    List<RawLeaderboardRow> findTop10ByEventIdOrderByPositionAsc(@Param("eventId") String eventId);

    @Query("""
        SELECT r FROM RawLeaderboardRow r
        WHERE r.id.eventId = :eventId
          AND r.position IS NOT NULL
    """)
    List<RawLeaderboardRow> findByEventId(@Param("eventId") String eventId);
}
