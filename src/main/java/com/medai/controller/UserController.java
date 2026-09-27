package com.medai.controller;

import com.medai.dto.UserProfileDto;
import com.medai.model.User;
import com.medai.model.UserInteraction;
import com.medai.repository.UserRepository;
import com.medai.service.PersonalisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PersonalisationService personalisationService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> updates) {

        if (updates.containsKey("fullName")) {
            user.setFullName((String) updates.get("fullName"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone((String) updates.get("phone"));
        }
        if (updates.containsKey("gender")) {
            user.setGender((String) updates.get("gender"));
        }
        if (updates.containsKey("dateOfBirth")) {
            user.setDateOfBirth(LocalDate.parse((String) updates.get("dateOfBirth")));
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/interactions")
    public ResponseEntity<List<UserInteraction>> getInteractions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(personalisationService.getInteractionHistory(user.getId(), limit));
    }

    private UserProfileDto toDto(User u) {
        return UserProfileDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .dateOfBirth(u.getDateOfBirth())
                .phone(u.getPhone())
                .gender(u.getGender())
                .personalisationEnabled(Boolean.TRUE.equals(u.getPersonalisation()))
                .createdAt(u.getCreatedAt())
                .lastLogin(u.getLastLogin())
                .build();
    }
}
