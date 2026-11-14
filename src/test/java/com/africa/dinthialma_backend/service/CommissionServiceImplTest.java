package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.dto.CreateCommissionRequest;
import com.africa.dinthialma_backend.tontine.dto.UpdateCommissionRequest;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import com.africa.dinthialma_backend.tontine.mapper.CommissionMapper;
import com.africa.dinthialma_backend.tontine.repository.TontineCommissionRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.impl.CommissionServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionServiceImpl")
class CommissionServiceImplTest {

  @Mock private TontineCommissionRepository commissionRepository;
  @Spy private CommissionMapper commissionMapper = Mappers.getMapper(CommissionMapper.class);
  @Mock private TontineRepository tontineRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private AuditService auditService;

  @InjectMocks private CommissionServiceImpl service;

  private User createur;
  private User etranger;
  private Tontine tontine;

  @BeforeEach
  void setUp() {
    createur = Fixtures.user("Créateur");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(createur);
    lenient()
        .when(userRepository.findByKeycloakId(createur.getKeycloakId()))
        .thenReturn(Optional.of(createur));
    lenient()
        .when(userRepository.findByKeycloakId(etranger.getKeycloakId()))
        .thenReturn(Optional.of(etranger));
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
  }

  private CreateCommissionRequest creation(CommissionType type, String valeur) {
    CreateCommissionRequest r = new CreateCommissionRequest();
    r.setType(type);
    r.setValeur(new BigDecimal(valeur));
    r.setDescription("Frais");
    return r;
  }

  private TontineCommission commission(CommissionType type) {
    return TontineCommission.builder()
        .id(UUID.randomUUID())
        .tontine(tontine)
        .type(type)
        .valeur(new BigDecimal("5"))
        .description("Ancienne")
        .build();
  }

  @Test
  @DisplayName("création par le créateur : enregistrée, auditée, convertie")
  void creation() throws Exception {
    when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(List.of());
    when(commissionRepository.save(any()))
        .thenAnswer(
            i -> {
              TontineCommission c = i.getArgument(0);
              c.setId(UUID.randomUUID());
              return c;
            });

    CommissionResponse r =
        service.createCommission(
            createur.getKeycloakId(),
            tontine.getId(),
            creation(CommissionType.POURCENTAGE_JACKPOT, "10"));

    assertThat(r.getType()).isEqualTo(CommissionType.POURCENTAGE_JACKPOT);
    assertThat(r.getValeur()).isEqualByComparingTo("10");
    assertThat(r.getTontineId()).isEqualTo(tontine.getId());
    verify(auditService).logCreate(eq(createur), eq("tontine_commissions"), any());
  }

  @Test
  @DisplayName("une seule commission par type -> 409")
  void doublonDeType() {
    when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(List.of(commission(CommissionType.POURCENTAGE_JACKPOT)));
    assertThatThrownBy(
            () ->
                service.createCommission(
                    createur.getKeycloakId(),
                    tontine.getId(),
                    creation(CommissionType.POURCENTAGE_JACKPOT, "3")))
        .isInstanceOf(ConflictException.class);
    verify(commissionRepository, never()).save(any());
  }

  @Test
  @DisplayName("création refusée à un tiers non SUPER_ADMIN (403), autorisée au SUPER_ADMIN")
  void controleAcces() throws Exception {
    when(roleRepository.existsByUserIdAndRole(etranger.getId(), UserRole.SUPER_ADMIN))
        .thenReturn(false);
    assertThatThrownBy(
            () ->
                service.createCommission(
                    etranger.getKeycloakId(),
                    tontine.getId(),
                    creation(CommissionType.FRAIS_ADHESION, "500")))
        .isInstanceOf(ForbiddenException.class);

    when(roleRepository.existsByUserIdAndRole(etranger.getId(), UserRole.SUPER_ADMIN))
        .thenReturn(true);
    when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(List.of());
    when(commissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    assertThat(
            service.createCommission(
                etranger.getKeycloakId(),
                tontine.getId(),
                creation(CommissionType.FRAIS_ADHESION, "500")))
        .isNotNull();
  }

  @Test
  @DisplayName("tontine ou utilisateur inconnu -> 404")
  void introuvables() {
    UUID inconnue = UUID.randomUUID();
    when(tontineRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                service.createCommission(
                    createur.getKeycloakId(),
                    inconnue,
                    creation(CommissionType.FRAIS_ADHESION, "1")))
        .isInstanceOf(NotFoundException.class);
    when(userRepository.findByKeycloakId("inconnu")).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                service.createCommission(
                    "inconnu", tontine.getId(), creation(CommissionType.FRAIS_ADHESION, "1")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("liste : membre autorisé, tiers refusé")
  void liste() throws Exception {
    PageRequest page = PageRequest.of(0, 20);
    User membre = Fixtures.user("Membre");
    when(userRepository.findByKeycloakId(membre.getKeycloakId())).thenReturn(Optional.of(membre));
    when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
            tontine.getId(), membre.getId()))
        .thenReturn(true);
    when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId(), page))
        .thenReturn(new PageImpl<>(List.of(commission(CommissionType.FRAIS_ADHESION))));

    assertThat(service.listCommissions(membre.getKeycloakId(), tontine.getId(), page).getContent())
        .hasSize(1);
    assertThat(
            service.listCommissions(createur.getKeycloakId(), tontine.getId(), page).getContent())
        .hasSize(1);

    when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
            tontine.getId(), etranger.getId()))
        .thenReturn(false);
    assertThatThrownBy(
            () -> service.listCommissions(etranger.getKeycloakId(), tontine.getId(), page))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("modification : seuls les champs fournis changent, chacun audité")
  void modification() throws Exception {
    TontineCommission c = commission(CommissionType.POURCENTAGE_JACKPOT);
    when(commissionRepository.findByIdAndTontine_IdAndDeletedAtIsNull(c.getId(), tontine.getId()))
        .thenReturn(Optional.of(c));
    when(commissionRepository.save(c)).thenReturn(c);

    UpdateCommissionRequest valeurSeule = new UpdateCommissionRequest();
    valeurSeule.setValeur(new BigDecimal("7"));
    CommissionResponse r =
        service.updateCommission(createur.getKeycloakId(), tontine.getId(), c.getId(), valeurSeule);
    assertThat(r.getValeur()).isEqualByComparingTo("7");
    assertThat(r.getDescription()).isEqualTo("Ancienne");
    verify(auditService).log(createur, "tontine_commissions", c.getId(), "valeur", "5", "7");

    UpdateCommissionRequest descriptionSeule = new UpdateCommissionRequest();
    descriptionSeule.setDescription("Nouvelle");
    assertThat(
            service
                .updateCommission(
                    createur.getKeycloakId(), tontine.getId(), c.getId(), descriptionSeule)
                .getDescription())
        .isEqualTo("Nouvelle");
  }

  @Test
  @DisplayName(
      "pourcentage du jackpot > 100 -> 400 à la création et à la modification ; 100 accepté")
  void pourcentageTropEleve() throws Exception {
    assertThatThrownBy(
            () ->
                service.createCommission(
                    createur.getKeycloakId(),
                    tontine.getId(),
                    creation(CommissionType.POURCENTAGE_JACKPOT, "100.01")))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.COMMISSION_PERCENTAGE_TOO_HIGH));

    TontineCommission c = commission(CommissionType.POURCENTAGE_JACKPOT);
    when(commissionRepository.findByIdAndTontine_IdAndDeletedAtIsNull(c.getId(), tontine.getId()))
        .thenReturn(Optional.of(c));
    UpdateCommissionRequest maj = new UpdateCommissionRequest();
    maj.setValeur(new BigDecimal("150"));
    assertThatThrownBy(
            () ->
                service.updateCommission(createur.getKeycloakId(), tontine.getId(), c.getId(), maj))
        .isInstanceOf(BadRequestException.class);
    assertThat(c.getValeur()).isEqualByComparingTo("5");
    verify(commissionRepository, never()).save(any());

    // Un montant fixe n'est pas plafonné à 100
    service.verifierCommissionsInitiales(
        List.of(
            creation(CommissionType.POURCENTAGE_JACKPOT, "100"),
            creation(CommissionType.FRAIS_FIXES_PAR_CYCLE, "2500")));
  }

  @Test
  @DisplayName("commissions initiales (B30) : absentes = rien ; type en double -> 409")
  void verificationInitiale() throws Exception {
    service.verifierCommissionsInitiales(null);
    service.verifierCommissionsInitiales(List.of());
    assertThatThrownBy(
            () ->
                service.verifierCommissionsInitiales(
                    List.of(
                        creation(CommissionType.FRAIS_ADHESION, "500"),
                        creation(CommissionType.FRAIS_ADHESION, "1000"))))
        .isInstanceOfSatisfying(
            ConflictException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.COMMISSION_ALREADY_EXISTS));
    verify(commissionRepository, never()).save(any());
  }

  @Test
  @DisplayName("commissions initiales (B30) : chacune enregistrée et auditée au nom du créateur")
  void enregistrementInitial() {
    when(commissionRepository.save(any()))
        .thenAnswer(
            i -> {
              TontineCommission c = i.getArgument(0);
              c.setId(UUID.randomUUID());
              return c;
            });

    service.enregistrerCommissionsInitiales(createur, tontine, null);
    verify(commissionRepository, never()).save(any());

    service.enregistrerCommissionsInitiales(
        createur,
        tontine,
        List.of(
            creation(CommissionType.POURCENTAGE_JACKPOT, "5"),
            creation(CommissionType.FRAIS_ADHESION, "1000")));

    ArgumentCaptor<TontineCommission> captor = ArgumentCaptor.forClass(TontineCommission.class);
    verify(commissionRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(TontineCommission::getType, TontineCommission::getTontine)
        .containsExactly(
            tuple(CommissionType.POURCENTAGE_JACKPOT, tontine),
            tuple(CommissionType.FRAIS_ADHESION, tontine));
    verify(auditService, times(2)).logCreate(eq(createur), eq("tontine_commissions"), any());
  }

  @Test
  @DisplayName("modification d'une commission inconnue -> 404")
  void modificationInconnue() {
    UUID id = UUID.randomUUID();
    when(commissionRepository.findByIdAndTontine_IdAndDeletedAtIsNull(id, tontine.getId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                service.updateCommission(
                    createur.getKeycloakId(), tontine.getId(), id, new UpdateCommissionRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("suppression : logique (deletedAt) et auditée")
  void suppression() throws Exception {
    TontineCommission c = commission(CommissionType.FRAIS_FIXES_PAR_CYCLE);
    when(commissionRepository.findByIdAndTontine_IdAndDeletedAtIsNull(c.getId(), tontine.getId()))
        .thenReturn(Optional.of(c));

    service.deleteCommission(createur.getKeycloakId(), tontine.getId(), c.getId());

    ArgumentCaptor<TontineCommission> captor = ArgumentCaptor.forClass(TontineCommission.class);
    verify(commissionRepository).save(captor.capture());
    assertThat(captor.getValue().getDeletedAt()).isNotNull();
    verify(auditService).logDelete(createur, "tontine_commissions", c.getId());
  }
}
