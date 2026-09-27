package com.medai.controller;

import com.medai.model.User;
import com.medai.service.PersonalisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/personalisation")
@RequiredArgsConstructor
public class PersonalisationController {

    private final PersonalisationService personalisationService;

    @PutMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@AuthenticationPrincipal User user) {
        boolean newState = personalisationService.togglePersonalisation(user.getId());
        return ResponseEntity.ok(Map.of(
                "personalisationEnabled", newState,
                "message", newState ? "Personalisation enabled" : "Personalisation disabled"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@AuthenticationPrincipal User user) {
        boolean status = personalisationService.getPersonalisationStatus(user.getId());
        return ResponseEntity.ok(Map.of("personalisationEnabled", status));
    }
}
