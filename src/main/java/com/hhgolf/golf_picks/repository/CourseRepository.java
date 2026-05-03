package com.hhgolf.golf_picks.repository;

import com.hhgolf.golf_picks.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {

    /** All courses that have at least one tournament attached, ordered by name. */
    @Query("""
        SELECT DISTINCT c FROM Course c
        WHERE EXISTS (SELECT 1 FROM Tournament t WHERE t.course = c)
        ORDER BY c.courseName ASC
    """)
    List<Course> findCoursesWithTournaments();
}
