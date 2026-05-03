package com.hhgolf.golf_picks.controller;

import com.hhgolf.golf_picks.model.User;
import com.hhgolf.golf_picks.repository.UserRepository;
import com.hhgolf.golf_picks.service.WatchlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
public class UserWatchlistController {

    private final WatchlistService watchlistService;
    private final UserRepository userRepository;

    public UserWatchlistController(WatchlistService watchlistService,
                                   UserRepository userRepository) {
        this.watchlistService = watchlistService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails ud) {
        return userRepository.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = resolveUser(ud);
        model.addAttribute("watchlistSummaries", watchlistService.getWatchlistSummaries(user));
        // keep raw list for the "already watching" check in search results
        model.addAttribute("watchlist", watchlistService.getWatchlist(user));
        return "dashboard";
    }

    @GetMapping("/search")
    public String search(@AuthenticationPrincipal UserDetails ud,
                         @RequestParam(required = false) String name,
                         Model model) {
        User user = resolveUser(ud);
        model.addAttribute("watchlistSummaries", watchlistService.getWatchlistSummaries(user));
        model.addAttribute("watchlist", watchlistService.getWatchlist(user));
        model.addAttribute("results", watchlistService.searchPlayers(name));
        model.addAttribute("searchName", name);
        return "dashboard";
    }

    @PostMapping("/watchlist/add/{playerId}")
    public String add(@AuthenticationPrincipal UserDetails ud,
                      @PathVariable String playerId,
                      @RequestParam(required = false) String redirectTo,
                      RedirectAttributes ra) {
        watchlistService.addToWatchlist(resolveUser(ud), playerId);
        ra.addFlashAttribute("successMessage", "Player added to your watchlist.");
        return "redirect:" + (redirectTo != null ? redirectTo : "/dashboard");
    }

    @PostMapping("/watchlist/remove/{playerId}")
    public String remove(@AuthenticationPrincipal UserDetails ud,
                         @PathVariable String playerId,
                         RedirectAttributes ra) {
        watchlistService.removeFromWatchlist(resolveUser(ud), playerId);
        ra.addFlashAttribute("successMessage", "Player removed from your watchlist.");
        return "redirect:/dashboard";
    }
}