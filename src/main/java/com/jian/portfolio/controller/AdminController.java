package com.jian.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jian.portfolio.service.ContactMessageService;

@Controller
public class AdminController {

    private final ContactMessageService contactMessageService;

    public AdminController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin/contacts")
    public String contacts(Model model) {
        model.addAttribute(
            "contacts",
            contactMessageService.findAll()
        );

        return "admin/contacts";
    }

    @PostMapping("/admin/contacts/{id}/delete")
    public String deleteContact(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            contactMessageService.delete(id);

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "문의가 삭제되었습니다."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                "errorMessage",
                e.getMessage()
            );
        }

        return "redirect:/admin/contacts";
    }
}