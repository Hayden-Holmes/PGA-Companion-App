package com.hhgolf.golf_picks.service;

import com.hhgolf.golf_picks.dto.TournamentResult;
import com.hhgolf.golf_picks.dto.WatchlistPlayerSummary;
import com.hhgolf.golf_picks.model.*;
import com.hhgolf.golf_picks.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private static final Set<String> KEY_STAT_IDS = Set.of(
        "sg_total", "sg_ott", "sg_app", "sg_arg", "sg_putt",
        "gir_pct", "fairway_pct", "driving_distance",
        "scrambling", "birdie_avg", "scoring_avg"
    );

    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final PlayerSeasonStatRepository statRepository;
    private final RawLeaderboardRowRepository leaderboardRepository;

    public PlayerService(PlayerRepository playerRepository,
                         RoundRepository roundRepository,
                         PlayerSeasonStatRepository statRepository,
                         RawLeaderboardRowRepository leaderboardRepository) {
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
        this.statRepository = statRepository;
        this.leaderboardRepository = leaderboardRepository;
    }

    public List<Player> searchPlayers(String name) {
        if (name == null || name.isBlank()) return List.of();
        String[] tokens = name.trim().split("\\s+");
        if (tokens.length >= 2) {
            return playerRepository.searchByTwoTokens(tokens[0], tokens[tokens.length - 1]);
        }
        return playerRepository.searchByName(tokens[0]);
    }

    public Player getPlayer(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
    }

    public List<String> getAllStatIds() {
        return statRepository.findAllStatIds();
    }

    public List<TournamentResult> getTournamentResults(String playerId) {
        List<Round> rounds = roundRepository.findByPlayerIdWithTournament(playerId);
        Map<String, List<Round>> byTournament = rounds.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTournament().getTournamentId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        return byTournament.entrySet().stream()
                .map(e -> buildResult(e.getKey(), e.getValue(), playerId))
                .limit(15)
                .collect(Collectors.toList());
    }

    public List<PlayerSeasonStat> getKeySeasonStats(String playerId) {
        Integer latestYear = statRepository.findLatestSeasonYear(playerId);
        if (latestYear == null) return List.of();
        List<PlayerSeasonStat> all = statRepository.findByPlayerIdAndYear(playerId, latestYear);
        List<PlayerSeasonStat> filtered = all.stream()
                .filter(s -> KEY_STAT_IDS.contains(s.getId().getStatId()))
                .collect(Collectors.toList());
        return filtered.isEmpty() ? all.stream().limit(12).collect(Collectors.toList()) : filtered;
    }

    public WatchlistPlayerSummary buildWatchlistSummary(Player player) {
        WatchlistPlayerSummary summary = new WatchlistPlayerSummary();
        summary.setPlayerId(player.getPlayerId());
        summary.setPlayerName(player.getPlayerName());
        summary.setCountry(player.getCountry());

        List<Round> recentRounds = roundRepository.findRecentRounds(player.getPlayerId())
                .stream().limit(8).collect(Collectors.toList());

        if (recentRounds.isEmpty()) {
            summary.setLastTournamentName("No data");
            summary.setLastScoreDisplay("-");
            summary.setTrend(WatchlistPlayerSummary.TrendDirection.FLAT);
            return summary;
        }

        // Group rounds by tournament, sorted most-recent first
        Map<String, List<Round>> byTournament = new LinkedHashMap<>();
        for (Round r : recentRounds) {
            byTournament.computeIfAbsent(r.getTournament().getTournamentId(), k -> new ArrayList<>()).add(r);
        }

        // Always show most recent tournament regardless of rounds played
        String latestId     = byTournament.keySet().iterator().next();
        List<Round> latestRounds = byTournament.get(latestId);
        TournamentResult latest  = buildResult(latestId, latestRounds, player.getPlayerId());
        summary.setLastTournamentName(latest.getTournamentName());
        summary.setLastScoreDisplay(latest.getScoreDisplay());
        summary.setSgTotal(latest.getAvgSgTotal());

        // For trend: only compare tournaments where the player completed ≥ 3 rounds (skip CUTs)
        List<String> completedIds = byTournament.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (completedIds.size() >= 2 && latest.getAvgSgTotal() != null) {
            String compareLatestId = completedIds.get(0);
            String comparePrevId   = completedIds.get(1);
            List<Round> compareLatest = byTournament.get(compareLatestId);
            List<Round> comparePrev   = byTournament.get(comparePrevId);
            TournamentResult tLatest  = buildResult(compareLatestId, compareLatest, player.getPlayerId());
            TournamentResult tPrev    = buildResult(comparePrevId,   comparePrev,   player.getPlayerId());
            if (tLatest.getAvgSgTotal() != null && tPrev.getAvgSgTotal() != null) {
                int cmp = tLatest.getAvgSgTotal().compareTo(tPrev.getAvgSgTotal());
                summary.setTrend(cmp > 0 ? WatchlistPlayerSummary.TrendDirection.UP
                        : cmp < 0      ? WatchlistPlayerSummary.TrendDirection.DOWN
                                       : WatchlistPlayerSummary.TrendDirection.FLAT);
            } else {
                summary.setTrend(WatchlistPlayerSummary.TrendDirection.FLAT);
            }
        } else {
            summary.setTrend(WatchlistPlayerSummary.TrendDirection.FLAT);
        }
        return summary;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private TournamentResult buildResult(String tournamentId, List<Round> rounds, String playerId) {
        TournamentResult r = new TournamentResult();
        Round first = rounds.get(0);
        Tournament tournament = first.getTournament();

        r.setTournamentId(tournamentId);
        r.setTournamentName(tournament.getTournamentName());
        r.setSeasonYear(tournament.getSeasonYear());
        r.setEndDate(tournament.getEndDate());
        r.setRoundsPlayed(rounds.size());

        // Course par — default to 72 (most common PGA Tour par) when not set
        Integer par = (tournament.getCourse() != null && tournament.getCourse().getPar() != null)
                ? tournament.getCourse().getPar()
                : 72;
        r.setCoursePar(par);

        // Raw round scores sorted by round number
        List<Round> sorted = rounds.stream()
                .sorted(Comparator.comparingInt(rd -> rd.getRoundNumber() != null ? rd.getRoundNumber() : 0))
                .collect(Collectors.toList());

        List<Integer> rawScores = sorted.stream().map(Round::getScore).collect(Collectors.toList());
        r.setRoundScores(rawScores);

        Integer rawTotal = rawScores.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        r.setTotalScore(rawTotal);

        // Score vs par per round and total
        if (par != null) {
            List<Integer> vsParRounds = rawScores.stream()
                    .map(s -> s != null ? s - par : null)
                    .collect(Collectors.toList());
            r.setRoundScoresVsPar(vsParRounds);
            r.setTotalScoreVsPar(rawTotal - (par * rounds.size()));
        }

        // Position and status from leaderboard — join via tournament.eventId
        if (tournament.getEventId() != null) {
            leaderboardRepository.findByEventIdAndPlayerId(tournament.getEventId(), playerId)
                    .ifPresent(row -> {
                        r.setPosition(row.getPosition());
                        r.setStatus(row.getStatus());
                    });
        }
        // Default status to FIN if not set
        if (r.getStatus() == null) r.setStatus("FIN");

        // Aggregated stats
        r.setTotalBirdies(sum(rounds, Round::getBirdies));
        r.setTotalBogeys(sum(rounds, Round::getBogeys));
        r.setTotalDoubleBogeys(sum(rounds, Round::getDoubleBogeys));

        r.setAvgGir(avg(rounds, Round::getGir));
        r.setAvgFairwaysHit(avg(rounds, Round::getFairwaysHit));
        r.setAvgDrivingDistance(avg(rounds, Round::getDrivingDistance));
        r.setAvgPutts(avg(rounds, Round::getPutts));
        r.setAvgSgTotal(avg(rounds, Round::getSgTotal));
        r.setAvgSgOtt(avg(rounds, Round::getSgOtt));
        r.setAvgSgApp(avg(rounds, Round::getSgApp));
        r.setAvgSgArg(avg(rounds, Round::getSgArg));
        r.setAvgSgPutt(avg(rounds, Round::getSgPutt));
        r.setAvgScrambling(avg(rounds, Round::getScrambling));
        return r;
    }

    @FunctionalInterface private interface DecimalGetter { BigDecimal get(Round r); }
    @FunctionalInterface private interface IntGetter     { Integer   get(Round r); }

    private BigDecimal avg(List<Round> rounds, DecimalGetter getter) {
        List<BigDecimal> vals = rounds.stream().map(getter::get)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (vals.isEmpty()) return null;
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(vals.size()), 3, RoundingMode.HALF_UP);
    }

    private Integer sum(List<Round> rounds, IntGetter getter) {
        return rounds.stream().map(getter::get)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }
}