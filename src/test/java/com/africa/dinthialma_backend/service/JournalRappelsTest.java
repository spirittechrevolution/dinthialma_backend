package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.notification.codeList.CanalRappel;
import com.africa.dinthialma_backend.notification.codeList.NatureRappel;
import com.africa.dinthialma_backend.notification.entity.RappelEnvoye;
import com.africa.dinthialma_backend.notification.repository.RappelEnvoyeRepository;
import com.africa.dinthialma_backend.notification.service.JournalRappels;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalRappels – anti-doublon des rappels (B26)")
class JournalRappelsTest {

  @Mock private RappelEnvoyeRepository repository;
  @InjectMocks private JournalRappels journal;

  @Test
  @DisplayName("première réservation : ligne enregistrée, envoi à faire")
  void premiere() {
    UUID echeance = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    assertThat(journal.reserver(echeance, user, NatureRappel.VEILLE, CanalRappel.WHATSAPP))
        .isTrue();
    ArgumentCaptor<RappelEnvoye> ligne = ArgumentCaptor.forClass(RappelEnvoye.class);
    verify(repository).saveAndFlush(ligne.capture());
    assertThat(ligne.getValue().getEcheanceId()).isEqualTo(echeance);
    assertThat(ligne.getValue().getUserId()).isEqualTo(user);
    assertThat(ligne.getValue().getNature()).isEqualTo(NatureRappel.VEILLE);
    assertThat(ligne.getValue().getCanal()).isEqualTo(CanalRappel.WHATSAPP);
  }

  @Test
  @DisplayName("doublon (index unique) : envoi déjà fait, aucune exception")
  void doublon() {
    when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq"));
    assertThat(
            journal.reserver(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NatureRappel.HEURES_AVANT,
                CanalRappel.IN_APP))
        .isFalse();
  }
}
