package com.hhgolf.golf_picks.controller;

import com.hhgolf.golf_picks.model.User;
import com.hhgolf.golf_picks.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("errorMessage", "Invalid username or password.");
        if (logout != null) model.addAttribute("successMessage", "You have been signed out.");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username already taken.");
            return "redirect:/register";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Account created! Please log in.");
        return "redirect:/login";
    }
} 
    
