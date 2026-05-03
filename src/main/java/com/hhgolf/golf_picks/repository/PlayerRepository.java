package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, String> {

    /** Single-token substring search (used when query has no spaces). */
    @Query("SELECT p FROM Player p WHERE LOWER(p.playerName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.playerName ASC")
    List<Player> searchByName(@Param("name") String name);

    /** Two-token AND search — both tokens must appear anywhere in the name. */
    @Query("""
        SELECT p FROM Player p
        WHERE LOWER(p.playerName) LIKE LOWER(CONCAT('%', :t1, '%'))
          AND LOWER(p.playerName) LIKE LOWER(CONCAT('%', :t2, '%'))
        ORDER BY p.playerName ASC
    """)
    List<Player> searchByTwoTokens(@Param("t1") String t1, @Param("t2") String t2);
}
