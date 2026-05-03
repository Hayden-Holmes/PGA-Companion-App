package com.hhgolf.golf_picks.service;

import com.hhgolf.golf_picks.dto.WatchlistPlayerSummary;
import com.hhgolf.golf_picks.model.Player;
import com.hhgolf.golf_picks.model.User;
import com.hhgolf.golf_picks.model.UserWatchlist;
import com.hhgolf.golf_picks.repository.PlayerRepository;
import com.hhgolf.golf_picks.repository.UserWatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchlistService {

    private final UserWatchlistRepository watchlistRepository;
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    public WatchlistService(UserWatchlistRepository watchlistRepository,
                            PlayerRepository playerRepository,
                            PlayerService playerService) {
        this.watchlistRepository = watchlistRepository;
        this.playerRepository = playerRepository;
        this.playerService = playerService;
    }

    public List<Player> getWatchlist(User user) {
        return watchlistRepository.findPlayersByUser(user);
    }

    /** Returns enriched summaries for the dashboard watchlist cards. */
    public List<WatchlistPlayerSummary> getWatchlistSummaries(User user) {
        return watchlistRepository.findPlayersByUser(user).stream()
                .map(playerService::buildWatchlistSummary)
                .collect(Collectors.toList());
    }

    public List<Player> searchPlayers(String name) {
        if (name == null || name.isBlank()) return List.of();
        String[] tokens = name.trim().split("\\s+");
        if (tokens.length >= 2) {
            return playerRepository.searchByTwoTokens(tokens[0], tokens[tokens.length - 1]);
        }
        return playerRepository.searchByName(tokens[0]);
    }

    @Transactional
    public void addToWatchlist(User user, String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        if (!watchlistRepository.existsByUserAndPlayer(user, player)) {
            watchlistRepository.save(new UserWatchlist(user, player));
        }
    }

    @Transactional
    public void removeFromWatchlist(User user, String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        watchlistRepository.deleteByUserAndPlayer(user, player);
    }
}