package com.jian.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jian.portfolio.entity.ContactMessage;
import com.jian.portfolio.service.ContactMessageService;

@Controller
public class HomeController {

    private final ContactMessageService contactMessageService;

    public HomeController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/contact")
    public String contact(
            ContactMessage message,
            RedirectAttributes redirectAttributes) {

        try {
            contactMessageService.save(message);

            redirectAttributes.addFlashAttribute(
                "contactSuccess",
                "문의가 정상적으로 접수되었습니다."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                "contactError",
                e.getMessage()
            );
        }

        return "redirect:/#contact";
    }
}