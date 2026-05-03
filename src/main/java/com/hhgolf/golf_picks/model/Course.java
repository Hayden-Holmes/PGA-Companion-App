package com.hhgolf.golf_picks.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @Column(name = "course_id")
    private String courseId;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "location")
    private String location;

    @Column(name = "par")
    private Integer par;

    @Column(name = "yardage")
    private Integer yardage;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Tournament> tournaments;
}
