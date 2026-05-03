package com.hhgolf.golf_picks.controller;

import com.hhgolf.golf_picks.model.Player;
import com.hhgolf.golf_picks.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/players/{playerId}")
    public String playerDetail(@PathVariable String playerId, Model model) {
        Player player = playerService.getPlayer(playerId);
        model.addAttribute("player", player);
        model.addAttribute("tournamentResults", playerService.getTournamentResults(playerId));
        model.addAttribute("seasonStats", playerService.getKeySeasonStats(playerId));
        return "player-detail";
    }

    // Legacy search page — kept for standalone search if needed
    @GetMapping("/players/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        List<Player> results = playerService.searchPlayers(q);
        model.addAttribute("results", results);
        model.addAttribute("query", q != null ? q : "");
        return "players/search";
    }
}