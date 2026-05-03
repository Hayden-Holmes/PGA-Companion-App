package com.hhgolf.golf_picks.service.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScoringResult {
    private double formRating;        // average SG total over last N rounds
    private double consistencyScore;  // lower std deviation = more consistent (0-100 scale)
    private String trend;// "improving", "declining", "stable"
    private double positioningScore;


}