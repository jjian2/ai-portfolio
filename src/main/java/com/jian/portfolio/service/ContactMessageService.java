package com.jian.portfolio.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jian.portfolio.entity.ContactMessage;
import com.jian.portfolio.repository.ContactMessageRepository;

@Service
@Transactional
public class ContactMessageService {

    private final ContactMessageRepository repository;

    public ContactMessageService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    public void save(ContactMessage message) {

        if (message.getName() == null || message.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        if (message.getEmail() == null || message.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }

        if (message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("문의 내용을 입력해주세요.");
        }

        message.setName(message.getName().trim());
        message.setEmail(message.getEmail().trim());
        message.setMessage(message.getMessage().trim());
        message.setCreatedAt(LocalDateTime.now());

        repository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ContactMessage> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ContactMessage findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("해당 문의를 찾을 수 없습니다.")
            );
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("해당 문의를 찾을 수 없습니다.");
        }

        repository.deleteById(id);
    }
}