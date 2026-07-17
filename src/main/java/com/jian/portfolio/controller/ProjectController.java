package com.jian.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectController {

    @GetMapping("/projects")
    public String projectList() {
        return "projects/list";
    }

    @GetMapping("/projects/matnuri")
    public String matnuriDetail() {
        return "projects/matnuri";
    }

    @GetMapping("/projects/yangdongi")
    public String yangdongiDetail() {
        return "projects/yangdongi";
    }
    @GetMapping("/projects/news-summary")
    public String newsSummaryDetail() {
        return "projects/news-summary";
    }
    @GetMapping("/projects/cover-letter-ai")
    public String coverLetterAiDetail() {
        return "projects/cover-letter-ai";
    }
    @GetMapping("/projects/cyber-crime-dashboard")
    public String cyberCrimeDashboardDetail() {
        return "projects/cyber-crime-dashboard";
    }
    @GetMapping("/projects/university-web")
    public String universityWebDetail() {
        return "projects/university-web";
    }
    @GetMapping("/projects/ai-literacy")
    public String aiLiteracyDetail() {
        return "projects/ai-literacy";
    }
    @GetMapping("/projects/recommendation-presentation")
    public String recommendationPresentationDetail() {
        return "projects/recommendation-presentation";
    }
    @GetMapping("/projects/ai-travel-planner")
    public String aiTravelPlanner() {
        return "projects/ai-travel-planner";
    }
}