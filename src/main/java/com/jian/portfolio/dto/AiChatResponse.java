package com.jian.portfolio.dto;

import java.util.List;

public class AiChatResponse {

    private String answer;
    private List<ProjectLink> relatedProjects;

    public AiChatResponse() {
    }

    public AiChatResponse(String answer) {
        this.answer = answer;
        this.relatedProjects = List.of();
    }

    public AiChatResponse(
            String answer,
            List<ProjectLink> relatedProjects) {

        this.answer = answer;
        this.relatedProjects = relatedProjects;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ProjectLink> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(
            List<ProjectLink> relatedProjects) {

        this.relatedProjects = relatedProjects;
    }
}