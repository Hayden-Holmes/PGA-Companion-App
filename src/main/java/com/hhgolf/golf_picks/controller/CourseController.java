package com.hhgolf.golf_picks.controller;

import com.hhgolf.golf_picks.dto.CourseFitResult;
import com.hhgolf.golf_picks.model.Course;
import com.hhgolf.golf_picks.model.User;
import com.hhgolf.golf_picks.repository.CourseRepository;
import com.hhgolf.golf_picks.repository.UserRepository;
import com.hhgolf.golf_picks.service.CourseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CourseController {

    private final CourseService    courseService;
    private final CourseRepository courseRepository;
    private final UserRepository   userRepository;

    public CourseController(CourseService courseService,
                            CourseRepository courseRepository,
                            UserRepository userRepository) {
        this.courseService    = courseService;
        this.courseRepository = courseRepository;
        this.userRepository   = userRepository;
    }

    private User resolveUser(UserDetails ud) {
        return userRepository.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/course-fit")
    public String courseFit(@AuthenticationPrincipal UserDetails ud,
                            @RequestParam(required = false) String courseId,
                            Model model) {

        User         user    = resolveUser(ud);
        List<Course> courses = courseService.listCoursesWithTournaments();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);

        if (courseId != null && !courseId.isBlank()) {
            Course selected = courseRepository.findById(courseId).orElse(null);
            model.addAttribute("selectedCourse", selected);

            CourseFitResult result = courseService.buildFitList(courseId, user);
            model.addAttribute("fitEntries",  result.getEntries());
            model.addAttribute("fitResult",   result);
        }

        return "course-fit";
    }
}
