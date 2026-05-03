package com.hhgolf.golf_picks.service.strategy;

import com.hhgolf.golf_picks.model.Round;
import java.util.List;

public interface ScoringStrategy {
    ScoringResult calculate(List<Round> rounds);
}