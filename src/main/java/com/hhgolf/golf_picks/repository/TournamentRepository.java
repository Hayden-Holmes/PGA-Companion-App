package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.Round;
import com.hhgolf.golf_picks.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, String> {

    @Query("""
        SELECT t FROM Tournament t
        JOIN FETCH t.course c
        WHERE c.courseId = :courseId
        ORDER BY t.endDate DESC
    """)
    List<Tournament> findByCourseId(@Param("courseId") String courseId);

    @Query("""
    SELECT r
    FROM Round r
    WHERE r.player.playerId = :playerId
      AND r.tournament.tournamentId = :tournamentId
""")
List<Round> findByPlayerIdAndTournamentId(@Param("playerId") String playerId,
                                          @Param("tournamentId") String tournamentId);
}
