package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @Column(name = "tournament_id")
    private String tournamentId;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "tournament_name")
    private String tournamentName;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY)
    private List<Round> rounds;
}
