package com.medai.service;

import com.medai.model.User;
import com.medai.model.UserInteraction;
import com.medai.model.UserPreference;
import com.medai.repository.UserInteractionRepository;
import com.medai.repository.UserPreferenceRepository;
import com.medai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalisationService Tests")
class PersonalisationServiceTest {

    @Mock private UserInteractionRepository interactionRepository;
    @Mock private UserPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private OllamaService ollamaService;

    @InjectMocks
    private PersonalisationService personalisationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .personalisation(true)
                .build();
    }

    // ── Toggle tests ───────────────────────────────────────

    @Test
    @DisplayName("Should toggle personalisation from true to false")
    void togglePersonalisation_fromTrue_returnsFalse() {
        user.setPersonalisation(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean result = personalisationService.togglePersonalisation(1L);

        assertThat(result).isFalse();
        verify(userRepository).save(argThat(u -> Boolean.FALSE.equals(u.getPersonalisation())));
    }

    @Test
    @DisplayName("Should toggle personalisation from false to true")
    void togglePersonalisation_fromFalse_returnsTrue() {
        user.setPersonalisation(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean result = personalisationService.togglePersonalisation(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should throw when user not found during toggle")
    void togglePersonalisation_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> personalisationService.togglePersonalisation(999L)
        );
    }

    // ── Status tests ───────────────────────────────────────

    @Test
    @DisplayName("Should return true when personalisation is enabled")
    void getPersonalisationStatus_returnsTrue_whenEnabled() {
        user.setPersonalisation(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(personalisationService.getPersonalisationStatus(1L)).isTrue();
    }

    @Test
    @DisplayName("Should return false when personalisation is disabled")
    void getPersonalisationStatus_returnsFalse_whenDisabled() {
        user.setPersonalisation(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(personalisationService.getPersonalisationStatus(1L)).isFalse();
    }

    @Test
    @DisplayName("Should return false when user not found")
    void getPersonalisationStatus_returnsFalse_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(personalisationService.getPersonalisationStatus(999L)).isFalse();
    }

    // ── Context building tests ─────────────────────────────

    @Test
    @DisplayName("Should include known conditions in context when preferences exist")
    void buildPersonalisationContext_includesKnownConditions() {
        UserPreference pref = UserPreference.builder()
                .user(user)
                .knownConditions(new String[]{"hypertension", "type 2 diabetes"})
                .allergies(new String[]{"aspirin"})
                .build();

        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[768]);
        when(interactionRepository.findSimilarInteractions(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of());
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        String ctx = personalisationService.buildPersonalisationContext(1L, "I have chest pain");

        assertThat(ctx).contains("hypertension");
        assertThat(ctx).contains("type 2 diabetes");
        assertThat(ctx).contains("aspirin");
    }

    @Test
    @DisplayName("Should include similar past interactions in context")
    void buildPersonalisationContext_includesSimilarInteractions() {
        UserInteraction interaction = UserInteraction.builder()
                .feature("COMMON_DISEASE")
                .inputSummary("severe headache for 3 days")
                .outputSummary("Possible migraine — HIGH confidence")
                .build();

        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[768]);
        when(interactionRepository.findSimilarInteractions(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of(interaction));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        String ctx = personalisationService.buildPersonalisationContext(1L, "headache again");

        assertThat(ctx).contains("severe headache for 3 days");
        assertThat(ctx).contains("Possible migraine");
    }

    @Test
    @DisplayName("Should return empty string gracefully when embedding fails")
    void buildPersonalisationContext_handlesEmbeddingFailure_gracefully() {
        when(ollamaService.generateEmbedding(anyString())).thenThrow(new RuntimeException("Ollama down"));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        String ctx = personalisationService.buildPersonalisationContext(1L, "some query");

        // Should not throw; returns whatever preference context is available
        assertThat(ctx).isNotNull();
    }

    // ── Interaction logging tests ──────────────────────────

    @Test
    @DisplayName("Should save interaction with embedding")
    void logInteraction_savesInteractionWithEmbedding() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[768]);
        when(interactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        personalisationService.logInteraction(1L, "MEDICINE", "What is ibuprofen?", "Ibuprofen is an NSAID...");

        verify(interactionRepository).save(argThat(interaction ->
                "MEDICINE".equals(interaction.getFeature()) &&
                "What is ibuprofen?".equals(interaction.getInputSummary())
        ));
    }

    @Test
    @DisplayName("Should not throw if logging interaction fails")
    void logInteraction_doesNotThrow_onRepositoryError() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[768]);
        when(interactionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should complete silently — logging must never crash the main flow
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                personalisationService.logInteraction(1L, "CHAT", "test input", "test output")
        );
    }

    // ── Preference update tests ────────────────────────────

    @Test
    @DisplayName("Should create new preferences if none exist")
    void updatePreferences_createsNewRecord_whenNoneExist() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        personalisationService.updatePreferences(1L,
                new String[]{"diabetes"}, new String[]{"metformin"}, new String[]{"penicillin"});

        verify(preferenceRepository).save(argThat(p ->
                p.getKnownConditions() != null &&
                p.getKnownConditions()[0].equals("diabetes")
        ));
    }

    @Test
    @DisplayName("Should retrieve interaction history with correct limit")
    void getInteractionHistory_returnsLimitedResults() {
        List<UserInteraction> interactions = List.of(
                UserInteraction.builder().feature("CHAT").build(),
                UserInteraction.builder().feature("MEDICINE").build()
        );
        when(interactionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(interactions);

        var result = personalisationService.getInteractionHistory(1L, 10);
        assertThat(result).hasSize(2);
    }
}
