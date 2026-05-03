package com.hhgolf.golf_picks.service;

import com.hhgolf.golf_picks.dto.CourseFitEntry;
import com.hhgolf.golf_picks.dto.CourseFitResult;
import com.hhgolf.golf_picks.dto.CourseWeightsResponse;
import com.hhgolf.golf_picks.model.*;
import com.hhgolf.golf_picks.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private static final int    RECENT_ROUND_LIMIT                 = 24;
    private static final int    MIN_HISTORY_ROWS_FOR_LEARNED_WEIGHTS = 12;

    @Value("${python.engine.url:http://localhost:8000}")
    private String pythonEngineUrl;

    private final CourseRepository          courseRepository;
    private final TournamentRepository      tournamentRepository;
    private final RoundRepository           roundRepository;
    private final UserWatchlistRepository   watchlistRepository;
    private final RawLeaderboardRowRepository leaderboardRepository;
    private final RestTemplate              restTemplate;

    public CourseService(CourseRepository courseRepository,
                         TournamentRepository tournamentRepository,
                         RoundRepository roundRepository,
                         UserWatchlistRepository watchlistRepository,
                         RawLeaderboardRowRepository leaderboardRepository,
                         RestTemplate restTemplate) {
        this.courseRepository      = courseRepository;
        this.tournamentRepository  = tournamentRepository;
        this.roundRepository       = roundRepository;
        this.watchlistRepository   = watchlistRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.restTemplate          = restTemplate;
    }

    public List<Course> listCoursesWithTournaments() {
        return courseRepository.findCoursesWithTournaments();
    }

    /**
     * For each player on the user's watchlist, calculate a course fit score
     * for the given course, then return a {@link CourseFitResult} that
     * includes both the ranked entries and the algorithm metadata.
     *
     * Weight resolution order:
     *   1. Python engine  (Ridge Regression + exponential time decay)
     *   2. Java Pearson   (correlation-based, no decay)
     *   3. Heuristic      (yardage tiers)
     */
    public CourseFitResult buildFitList(String courseId, User user) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return new CourseFitResult(
                    List.of(),
                    new double[]{0.25, 0.25, 0.25, 0.25},
                    "heuristic", "low", 0, null
            );
        }

        List<Tournament> courseTournaments = tournamentRepository.findByCourseId(courseId);
        Set<String> courseTournamentIds = courseTournaments.stream()
                .map(Tournament::getTournamentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ── Resolve weights ──────────────────────────────────────────────────
        WeightsBundle bundle = resolveWeights(course, courseTournaments);
        double[] weights = bundle.weights();

        List<Player> watchlist = watchlistRepository.findPlayersByUser(user);

        List<CourseFitEntry> entries = watchlist.stream()
                .map(player -> buildEntry(player, course, courseTournamentIds, weights))
                .sorted(Comparator.comparingDouble(CourseFitEntry::getFitScore).reversed())
                .collect(Collectors.toList());

        return new CourseFitResult(
                entries,
                weights,
                bundle.algorithm(),
                bundle.confidence(),
                bundle.dataPoints(),
                bundle.rSquared()
        );
    }

    // ── Weight resolution ────────────────────────────────────────────────────

    /**
     * Try Python engine first; fall back through Pearson then heuristic.
     */
    private WeightsBundle resolveWeights(Course course, List<Tournament> courseTournaments) {
        // 1. Python Ridge Regression engine
        WeightsBundle pythonResult = callPythonEngine(course.getCourseId());
        if (pythonResult != null) {
            log.info("Using Python Ridge Regression weights for course {}  (R²={}, n={}, confidence={})",
                    course.getCourseId(),
                    pythonResult.rSquared(),
                    pythonResult.dataPoints(),
                    pythonResult.confidence());
            return pythonResult;
        }

        // 2. Java Pearson correlation (original algorithm)
        log.info("Python engine unavailable — falling back to Java Pearson for course {}", course.getCourseId());
        return pearsonWeights(course, courseTournaments);
    }

    private WeightsBundle callPythonEngine(String courseId) {
        try {
            String url = pythonEngineUrl + "/weights/" + courseId;
            ResponseEntity<CourseWeightsResponse> resp =
                    restTemplate.getForEntity(url, CourseWeightsResponse.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }

            CourseWeightsResponse body = resp.getBody();
            return new WeightsBundle(
                    body.toWeightsArray(),
                    body.getAlgorithm(),
                    body.getConfidence(),
                    body.getDataPoints(),
                    body.getRSquared()
            );
        } catch (Exception e) {
            log.warn("Python engine call failed for course {}: {}", courseId, e.getMessage());
            return null;
        }
    }

    /**
     * Original Pearson-correlation weight learning, kept as fallback.
     */
    private WeightsBundle pearsonWeights(Course course, List<Tournament> courseTournaments) {
        List<HistoricalTopFinishRow> rows = new ArrayList<>();

        for (Tournament tournament : courseTournaments) {
            if (tournament == null
                    || tournament.getTournamentId() == null
                    || tournament.getEventId() == null) {
                continue;
            }

            List<RawLeaderboardRow> topRows =
                    leaderboardRepository.findTop10ByEventIdOrderByPositionAsc(tournament.getEventId());

            for (RawLeaderboardRow row : topRows) {
                if (row.getId() == null || row.getId().getPlayerId() == null || row.getPosition() == null) continue;

                List<Round> rounds = sortRoundsByRoundNumber(
                        tournamentRepository.findByPlayerIdAndTournamentId(
                                row.getId().getPlayerId(), tournament.getTournamentId()));

                if (rounds.isEmpty()) continue;

                BigDecimal avgOtt  = avg(rounds, Round::getSgOtt);
                BigDecimal avgApp  = avg(rounds, Round::getSgApp);
                BigDecimal avgArg  = avg(rounds, Round::getSgArg);
                BigDecimal avgPutt = avg(rounds, Round::getSgPutt);

                if (avgOtt == null || avgApp == null || avgArg == null || avgPutt == null) continue;

                rows.add(new HistoricalTopFinishRow(
                        row.getPosition(),
                        avgOtt.doubleValue(), avgApp.doubleValue(),
                        avgArg.doubleValue(), avgPutt.doubleValue()));
            }
        }

        if (rows.size() < MIN_HISTORY_ROWS_FOR_LEARNED_WEIGHTS) {
            double[] w = heuristicWeights(course);
            return new WeightsBundle(w, "heuristic", "low", rows.size(), null);
        }

        double[] finishSignal = rows.stream()
                .mapToDouble(r -> -r.finishPosition())
                .toArray();

        double ottCorr  = Math.abs(pearsonCorrelation(rows.stream().mapToDouble(HistoricalTopFinishRow::sgOtt).toArray(),  finishSignal));
        double appCorr  = Math.abs(pearsonCorrelation(rows.stream().mapToDouble(HistoricalTopFinishRow::sgApp).toArray(),  finishSignal));
        double argCorr  = Math.abs(pearsonCorrelation(rows.stream().mapToDouble(HistoricalTopFinishRow::sgArg).toArray(),  finishSignal));
        double puttCorr = Math.abs(pearsonCorrelation(rows.stream().mapToDouble(HistoricalTopFinishRow::sgPutt).toArray(), finishSignal));

        double sum = ottCorr + appCorr + argCorr + puttCorr;
        if (sum <= 0.0 || Double.isNaN(sum)) {
            double[] w = heuristicWeights(course);
            return new WeightsBundle(w, "heuristic", "low", rows.size(), null);
        }

        double[] w = new double[]{ ottCorr / sum, appCorr / sum, argCorr / sum, puttCorr / sum };
        return new WeightsBundle(w, "pearson", "medium", rows.size(), null);
    }

    private double[] heuristicWeights(Course course) {
        int yardage = course.getYardage() != null ? course.getYardage() : 7100;
        if (yardage >= 7300) return new double[]{0.40, 0.30, 0.10, 0.20};
        if (yardage >= 6900) return new double[]{0.25, 0.32, 0.13, 0.30};
        return new double[]{0.15, 0.32, 0.20, 0.33};
    }

    // ── Entry builder ────────────────────────────────────────────────────────

    private CourseFitEntry buildEntry(Player player,
                                      Course course,
                                      Set<String> courseTournamentIds,
                                      double[] weights) {
        CourseFitEntry e = new CourseFitEntry();
        e.setPlayerId(player.getPlayerId());
        e.setPlayerName(player.getPlayerName());
        e.setCountry(player.getCountry());

        List<Round> allRounds = sortRoundsMostRecentFirst(
                roundRepository.findByPlayerIdWithTournament(player.getPlayerId()));

        List<Round> courseRounds = allRounds.stream()
                .filter(r -> r.getTournament() != null
                        && courseTournamentIds.contains(r.getTournament().getTournamentId()))
                .collect(Collectors.toList());

        List<Round> recentRounds = allRounds.stream()
                .limit(RECENT_ROUND_LIMIT)
                .collect(Collectors.toList());

        BigDecimal avgOtt  = avg(recentRounds, Round::getSgOtt);
        BigDecimal avgApp  = avg(recentRounds, Round::getSgApp);
        BigDecimal avgArg  = avg(recentRounds, Round::getSgArg);
        BigDecimal avgPutt = avg(recentRounds, Round::getSgPutt);
        BigDecimal avgTot  = avg(recentRounds, Round::getSgTotal);

        e.setAvgSgOtt(avgOtt);
        e.setAvgSgApp(avgApp);
        e.setAvgSgArg(avgArg);
        e.setAvgSgPutt(avgPutt);
        e.setAvgSgTotal(avgTot);

        e.setWeightedSgOtt(scaledBd(avgOtt,  weights[0]));
        e.setWeightedSgApp(scaledBd(avgApp,  weights[1]));
        e.setWeightedSgArg(scaledBd(avgArg,  weights[2]));
        e.setWeightedSgPutt(scaledBd(avgPutt, weights[3]));

        double fitScore = computeRecentFormFitScore(avgOtt, avgApp, avgArg, avgPutt, weights);

        // Course history bonus/penalty: up to ±1.5 pts
        double historyBonus = 0.0;
        if (!courseRounds.isEmpty()) {
            Map<String, List<Round>> byTournament = courseRounds.stream()
                    .filter(r -> r.getTournament() != null && r.getTournament().getTournamentId() != null)
                    .collect(Collectors.groupingBy(r -> r.getTournament().getTournamentId()));

            List<Integer> totalVsPar = new ArrayList<>();
            List<Integer> finishes   = new ArrayList<>();
            List<Integer> scoreHist  = new ArrayList<>();

            for (Map.Entry<String, List<Round>> te : byTournament.entrySet()) {
                List<Round> tRounds    = sortRoundsByRoundNumber(te.getValue());
                Tournament  tournament = tRounds.get(0).getTournament();
                Integer     par        = (tournament != null
                        && tournament.getCourse() != null
                        && tournament.getCourse().getPar() != null)
                        ? tournament.getCourse().getPar() : 72;

                List<Integer> roundScores = tRounds.stream()
                        .map(Round::getScore).filter(Objects::nonNull).collect(Collectors.toList());

                if (par != null && !roundScores.isEmpty()) {
                    int rawTotal = roundScores.stream().mapToInt(Integer::intValue).sum();
                    int vsPar    = rawTotal - (par * roundScores.size());
                    totalVsPar.add(vsPar);
                    scoreHist.add(vsPar);
                }

                String eventId = tournament != null ? tournament.getEventId() : null;
                if (eventId != null) {
                    leaderboardRepository.findByEventIdAndPlayerId(eventId, player.getPlayerId())
                            .ifPresent(row -> { if (row.getPosition() != null) finishes.add(row.getPosition()); });
                }
            }

            e.setCourseAppearances(byTournament.size());

            if (!totalVsPar.isEmpty()) {
                double avgVP = totalVsPar.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                e.setAvgScoreVsPar(avgVP);
                historyBonus = Math.max(-1.5, Math.min(1.5, avgVP * -0.5 / 3.0));
            }

            if (!finishes.isEmpty()) {
                e.setBestFinish(finishes.stream().mapToInt(Integer::intValue).min().orElse(0));
            }

            e.setCourseScoreHistory(scoreHist);
        } else {
            e.setCourseAppearances(0);
        }

        fitScore = Math.max(0.0, Math.min(10.0, fitScore + historyBonus));
        e.setFitScore(fitScore);
        e.setFitGrade(toGrade(fitScore));
        e.setFitSummary(buildSummary(e, weights, course));

        return e;
    }

    // ── Scoring ──────────────────────────────────────────────────────────────

    private double computeRecentFormFitScore(BigDecimal avgOtt, BigDecimal avgApp,
                                              BigDecimal avgArg, BigDecimal avgPutt,
                                              double[] weights) {
        double statScore  = 0.0;
        double usedWeight = 0.0;

        if (avgOtt  != null) { statScore += avgOtt.doubleValue()  * weights[0]; usedWeight += weights[0]; }
        if (avgApp  != null) { statScore += avgApp.doubleValue()  * weights[1]; usedWeight += weights[1]; }
        if (avgArg  != null) { statScore += avgArg.doubleValue()  * weights[2]; usedWeight += weights[2]; }
        if (avgPutt != null) { statScore += avgPutt.doubleValue() * weights[3]; usedWeight += weights[3]; }

        if (usedWeight == 0.0) return 0.0;

        double normalizedComposite = statScore / usedWeight;
        return normalise(normalizedComposite, -2.5, 2.5, 0.0, 10.0);
    }

    private String buildSummary(CourseFitEntry e, double[] weights, Course course) {
        int yardage   = course.getYardage() != null ? course.getYardage() : 7100;
        String cType  = yardage >= 7300 ? "long" : yardage >= 6900 ? "balanced" : "short";

        Map<String, Double> weighted = new LinkedHashMap<>();
        if (e.getAvgSgOtt()  != null) weighted.put("off-the-tee",       e.getAvgSgOtt().doubleValue()  * weights[0]);
        if (e.getAvgSgApp()  != null) weighted.put("approach",          e.getAvgSgApp().doubleValue()  * weights[1]);
        if (e.getAvgSgArg()  != null) weighted.put("around-the-green",  e.getAvgSgArg().doubleValue()  * weights[2]);
        if (e.getAvgSgPutt() != null) weighted.put("putting",           e.getAvgSgPutt().doubleValue() * weights[3]);

        if (weighted.isEmpty()) return "Insufficient data";

        String best   = weighted.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("overall");
        String prefix = e.getFitScore() >= 7 ? "Strong " : e.getFitScore() >= 5 ? "Decent " : "Weak ";
        String hist   = e.getCourseAppearances() > 0
                ? " — " + e.getCourseAppearances() + " prior appearance" + (e.getCourseAppearances() > 1 ? "s" : "")
                : " — no course history";

        return prefix + best + " on this " + cType + " track" + hist;
    }

    // ── Sorting ──────────────────────────────────────────────────────────────

    private List<Round> sortRoundsMostRecentFirst(List<Round> rounds) {
        if (rounds == null) return List.of();
        return rounds.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((Round r) -> tournamentEndDateOrMin(r.getTournament()), Comparator.reverseOrder())
                        .thenComparing((Round r) -> roundNumberOrMin(r), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<Round> sortRoundsByRoundNumber(List<Round> rounds) {
        if (rounds == null) return List.of();
        return rounds.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(this::roundNumberOrMax))
                .collect(Collectors.toList());
    }

    private Date tournamentEndDateOrMin(Tournament t) {
        if (t == null || t.getEndDate() == null) return new Date(0L);
        return java.sql.Date.valueOf(t.getEndDate());
    }

    private int roundNumberOrMin(Round r) {
        return r != null && r.getRoundNumber() != null ? r.getRoundNumber() : Integer.MIN_VALUE;
    }

    private int roundNumberOrMax(Round r) {
        return r != null && r.getRoundNumber() != null ? r.getRoundNumber() : Integer.MAX_VALUE;
    }

    // ── Maths ────────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface BdGetter { BigDecimal get(Round r); }

    private BigDecimal avg(List<Round> rounds, BdGetter getter) {
        List<BigDecimal> vals = rounds.stream()
                .map(getter::get).filter(Objects::nonNull).collect(Collectors.toList());
        if (vals.isEmpty()) return null;
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(vals.size()), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal scaledBd(BigDecimal val, double weight) {
        if (val == null) return null;
        return val.multiply(BigDecimal.valueOf(weight)).setScale(3, RoundingMode.HALF_UP);
    }

    private double normalise(double value, double inMin, double inMax, double outMin, double outMax) {
        double clamped = Math.max(inMin, Math.min(inMax, value));
        return outMin + (clamped - inMin) / (inMax - inMin) * (outMax - outMin);
    }

    private double pearsonCorrelation(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 2) return 0.0;
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);
        double num = 0, sqX = 0, sqY = 0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX, dy = y[i] - meanY;
            num += dx * dy; sqX += dx * dx; sqY += dy * dy;
        }
        double denom = Math.sqrt(sqX * sqY);
        return denom == 0.0 ? 0.0 : num / denom;
    }

    private String toGrade(double score) {
        if (score >= 9.0) return "A+";
        if (score >= 8.0) return "A";
        if (score >= 7.0) return "B+";
        if (score >= 6.0) return "B";
        if (score >= 5.0) return "C+";
        if (score >= 4.0) return "C";
        return "D";
    }

    // ── Records ──────────────────────────────────────────────────────────────

    private record WeightsBundle(
            double[] weights,
            String algorithm,
            String confidence,
            int dataPoints,
            Double rSquared
    ) {}

    private record HistoricalTopFinishRow(
            int finishPosition,
            double sgOtt, double sgApp, double sgArg, double sgPutt
    ) {}
}
