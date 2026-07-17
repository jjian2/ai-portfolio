package com.jian.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.jian.portfolio.dto.AiChatRequest;
import com.jian.portfolio.dto.AiChatResponse;
import com.jian.portfolio.service.AiChatService;

@RestController
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/api/ai/chat")
    public ResponseEntity<AiChatResponse> chat(
            @RequestBody AiChatRequest request) {

        AiChatResponse response =
                aiChatService.ask(request.getMessage());

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AiChatResponse> handleBadRequest(
            IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(new AiChatResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiChatResponse> handleServerError(
            Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .body(
                    new AiChatResponse(
                        "AI 답변을 생성하는 중 오류가 발생했습니다."
                    )
                );
    }
}