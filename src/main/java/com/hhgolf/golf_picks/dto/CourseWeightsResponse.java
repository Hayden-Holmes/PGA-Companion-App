package com.hhgolf.golf_picks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Deserialised response from the Python course-fit engine. */
public class CourseWeightsResponse {

    @JsonProperty("sg_ott")
    private double sgOtt;

    @JsonProperty("sg_app")
    private double sgApp;

    @JsonProperty("sg_arg")
    private double sgArg;

    @JsonProperty("sg_putt")
    private double sgPutt;

    @JsonProperty("algorithm")
    private String algorithm;

    @JsonProperty("data_points")
    private int dataPoints;

    @JsonProperty("r_squared")
    private Double rSquared;

    @JsonProperty("confidence")
    private String confidence;

    @JsonProperty("decay_half_life_days")
    private Integer decayHalfLifeDays;

    public double getSgOtt()            { return sgOtt; }
    public double getSgApp()            { return sgApp; }
    public double getSgArg()            { return sgArg; }
    public double getSgPutt()           { return sgPutt; }
    public String getAlgorithm()        { return algorithm; }
    public int    getDataPoints()       { return dataPoints; }
    public Double getRSquared()         { return rSquared; }
    public String getConfidence()       { return confidence; }
    public Integer getDecayHalfLifeDays() { return decayHalfLifeDays; }

    public double[] toWeightsArray() {
        return new double[]{ sgOtt, sgApp, sgArg, sgPutt };
    }
}
