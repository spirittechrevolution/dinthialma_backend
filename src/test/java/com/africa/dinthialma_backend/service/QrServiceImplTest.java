package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.me.dto.MonQrResponse;
import com.africa.dinthialma_backend.me.dto.QrResolutionResponse;
import com.africa.dinthialma_backend.me.service.impl.QrServiceImpl;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrServiceImpl")
class QrServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private CotisationRepository cotisationRepository;
  @Mock private TontineRepository tontineRepository;
  @InjectMocks private QrServiceImpl service;

  private User moi;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "appUrl", "https://app.test/");
    moi = Fixtures.user("Gestionnaire");
    lenient()
        .when(userRepository.findByKeycloakId(moi.getKeycloakId()))
        .thenReturn(Optional.of(moi));
  }

  @Test
  @DisplayName("mon QR : jeton opaque m_… créé une fois puis stable, URL sans double barre")
  void monQr() throws Exception {
    when(userRepository.existsByQrToken(anyString())).thenReturn(true, false);

    MonQrResponse r = service.getMonQr(moi.getKeycloakId());

    assertThat(r.getQrToken()).startsWith("m_").hasSize(24).doesNotContain(moi.getPhone());
    assertThat(r.getUrl()).isEqualTo("https://app.test/q/" + r.getQrToken());
    verify(userRepository).save(moi);

    assertThat(service.getMonQr(moi.getKeycloakId()).getQrToken()).isEqualTo(r.getQrToken());
    verify(userRepository, org.mockito.Mockito.times(1)).save(moi);
  }

  @Test
  @DisplayName("régénération : nouveau jeton différent de l'ancien")
  void regeneration() throws Exception {
    moi.setQrToken("m_ancien");
    String nouveau = service.regenererMonQr(moi.getKeycloakId()).getQrToken();
    assertThat(nouveau).startsWith("m_").isNotEqualTo("m_ancien");
    assertThat(moi.getQrToken()).isEqualTo(nouveau);
  }

  @Test
  @DisplayName(
      "résolution d'un membre : identité sans téléphone, seulement mes tontines, paiement du tour")
  void resolutionMembre() throws Exception {
    User scanne = Fixtures.user("Scanné");
    Tontine maTontine = Fixtures.tontine(moi);
    Tontine maTontineSansTour = Fixtures.tontine(moi);
    Tontine autreTontine = Fixtures.tontine(Fixtures.user("Autre"));
    TontineMembre m1 = Fixtures.membre(maTontine, scanne, 1);
    TontineMembre m2 = Fixtures.membre(maTontineSansTour, scanne, 2);
    TontineMembre m3 = Fixtures.membre(autreTontine, scanne, 1);
    CycleTontine tour = Fixtures.cycle(maTontine, 1, CycleStatut.EN_COURS);
    when(userRepository.findByQrTokenAndDeletedAtIsNull("m_abc")).thenReturn(Optional.of(scanne));
    when(membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                scanne.getId()))
        .thenReturn(List.of(m1, m2, m3));
    when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            maTontine.getId(), CycleStatut.EN_COURS))
        .thenReturn(Optional.of(tour));
    when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            maTontineSansTour.getId(), CycleStatut.EN_COURS))
        .thenReturn(Optional.empty());
    when(cotisationRepository.existsActiveByCycleAndMembre(tour.getId(), m1.getId()))
        .thenReturn(true);

    QrResolutionResponse r = service.resoudre(moi.getKeycloakId(), "m_abc");

    assertThat(r.getType()).isEqualTo("MEMBRE");
    assertThat(r.getUser().getFirstName()).isEqualTo("Scanné");
    assertThat(r.getDansMesTontines()).hasSize(2);
    QrResolutionResponse.MembreDansMaTontine avecTour = r.getDansMesTontines().get(0);
    assertThat(avecTour.getMembreId()).isEqualTo(m1.getId());
    assertThat(avecTour.getCycleEnCoursId()).isEqualTo(tour.getId());
    assertThat(avecTour.getAPayeCycleEnCours()).isTrue();
    assertThat(r.getDansMesTontines().get(1).getAPayeCycleEnCours()).isNull();
  }

  @Test
  @DisplayName("résolution d'un QR d'invitation t_… : renvoie le code de la tontine")
  void resolutionInvitation() throws Exception {
    Tontine t = Fixtures.tontine(moi);
    t.setCodeInvitation("ABCD-EFGH");
    when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull("ABCD-EFGH"))
        .thenReturn(Optional.of(t));

    QrResolutionResponse r = service.resoudre(moi.getKeycloakId(), "t_ABCD-EFGH");

    assertThat(r.getType()).isEqualTo("TONTINE");
    assertThat(r.getInvitationCode()).isEqualTo("ABCD-EFGH");
    assertThat(r.getDansMesTontines()).isEmpty();
  }

  @Test
  @DisplayName("jetons inconnus, révoqués ou mal préfixés -> 404")
  void introuvables() {
    when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull("XXXX"))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resoudre(moi.getKeycloakId(), "t_XXXX"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.resoudre(moi.getKeycloakId(), "221770000001"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.resoudre(moi.getKeycloakId(), null))
        .isInstanceOf(NotFoundException.class);
    when(userRepository.findByQrTokenAndDeletedAtIsNull("m_revoque")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resoudre(moi.getKeycloakId(), "m_revoque"))
        .isInstanceOf(NotFoundException.class);
    verify(membreRepository, never())
        .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.any());
  }
}
