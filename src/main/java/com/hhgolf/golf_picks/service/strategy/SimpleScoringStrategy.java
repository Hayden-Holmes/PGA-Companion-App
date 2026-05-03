package com.hhgolf.golf_picks.service.strategy;

    // import com.hhgolf.golf_picks.model.Round;
    // import co.hhgolf.golf_picks.service.strategy.ScoringResult;
    // import java.util.List;



    // public class SimpleScoringStrategy implements ScoringStrategy {

    //     // @Override
    //     public ScoringResult calculate(List<Round> rounds) {
    //         if (rounds == null || rounds.isEmpty()) {
    //             return new ScoringResult(0, 0, "stable");
    //         }

    //        double scoringTotal = 0;
    //         for (Round round : rounds) {
    //             scoringTotal += round.getScoring();
    //         }
    //         double formRating = scoringTotal / rounds.size();

    //         // For simplicity, we will not calculate consistency and trend in this example
    //         return new ScoringResult(formRating, 0, "stable");
    // }
