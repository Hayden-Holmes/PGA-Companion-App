package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @Column(name = "round_id")
    private String roundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "round_date")
    private LocalDate roundDate;

    @Column(name = "score")
    private Integer score;

    @Column(name = "gir")
    private BigDecimal gir;

    @Column(name = "fairways_hit")
    private BigDecimal fairwaysHit;

    @Column(name = "driving_distance")
    private BigDecimal drivingDistance;

    @Column(name = "putts")
    private BigDecimal putts;

    @Column(name = "sg_total")
    private BigDecimal sgTotal;

    @Column(name = "sg_ott")
    private BigDecimal sgOtt;

    @Column(name = "sg_app")
    private BigDecimal sgApp;

    @Column(name = "sg_arg")
    private BigDecimal sgArg;

    @Column(name = "sg_putt")
    private BigDecimal sgPutt;

    @Column(name = "scrambling")
    private BigDecimal scrambling;

    @Column(name = "putts_per_gir")
    private BigDecimal puttsPerGir;

    @Column(name = "birdies")
    private Integer birdies;

    @Column(name = "pars")
    private Integer pars;

    @Column(name = "bogeys")
    private Integer bogeys;

    @Column(name = "double_bogeys")
    private Integer doubleBogeys;

    public Round() {}

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public BigDecimal getGir() { return gir; }
    public void setGir(BigDecimal gir) { this.gir = gir; }

    public BigDecimal getFairwaysHit() { return fairwaysHit; }
    public void setFairwaysHit(BigDecimal fairwaysHit) { this.fairwaysHit = fairwaysHit; }

    public BigDecimal getDrivingDistance() { return drivingDistance; }
    public void setDrivingDistance(BigDecimal drivingDistance) { this.drivingDistance = drivingDistance; }

    public BigDecimal getPutts() { return putts; }
    public void setPutts(BigDecimal putts) { this.putts = putts; }

    public BigDecimal getSgTotal() { return sgTotal; }
    public void setSgTotal(BigDecimal sgTotal) { this.sgTotal = sgTotal; }

    public BigDecimal getSgOtt() { return sgOtt; }
    public void setSgOtt(BigDecimal sgOtt) { this.sgOtt = sgOtt; }

    public BigDecimal getSgApp() { return sgApp; }
    public void setSgApp(BigDecimal sgApp) { this.sgApp = sgApp; }

    public BigDecimal getSgArg() { return sgArg; }
    public void setSgArg(BigDecimal sgArg) { this.sgArg = sgArg; }

    public BigDecimal getSgPutt() { return sgPutt; }
    public void setSgPutt(BigDecimal sgPutt) { this.sgPutt = sgPutt; }

    public BigDecimal getScrambling() { return scrambling; }
    public void setScrambling(BigDecimal scrambling) { this.scrambling = scrambling; }

    public BigDecimal getPuttsPerGir() { return puttsPerGir; }
    public void setPuttsPerGir(BigDecimal puttsPerGir) { this.puttsPerGir = puttsPerGir; }

    public Integer getBirdies() { return birdies; }
    public void setBirdies(Integer birdies) { this.birdies = birdies; }

    public Integer getPars() { return pars; }
    public void setPars(Integer pars) { this.pars = pars; }

    public Integer getBogeys() { return bogeys; }
    public void setBogeys(Integer bogeys) { this.bogeys = bogeys; }

    public Integer getDoubleBogeys() { return doubleBogeys; }
    public void setDoubleBogeys(Integer doubleBogeys) { this.doubleBogeys = doubleBogeys; }
}
