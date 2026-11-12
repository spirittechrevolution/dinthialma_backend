package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.security.AccessControl;
import com.africa.dinthialma_backend.support.Fixtures;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessControl")
class AccessControlTest {

  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @InjectMocks private AccessControl accessControl;

  @Test
  @DisplayName("utilisateur inconnu -> 404")
  void inconnu() {
    when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> accessControl.utilisateur("x")).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("exigerUnRole : un rôle parmi ceux demandés suffit ; aucun -> 403")
  void exigerUnRole() throws Exception {
    User u = Fixtures.user("Admin");
    when(userRepository.findByKeycloakId(u.getKeycloakId())).thenReturn(Optional.of(u));
    when(roleRepository.existsByUserIdAndRole(u.getId(), UserRole.SUPER_ADMIN)).thenReturn(false);
    when(roleRepository.existsByUserIdAndRole(u.getId(), UserRole.ADMIN)).thenReturn(true);
    assertThat(accessControl.exigerUnRole(u.getKeycloakId(), UserRole.SUPER_ADMIN, UserRole.ADMIN))
        .isSameAs(u);

    assertThatThrownBy(() -> accessControl.exigerUnRole(u.getKeycloakId(), UserRole.SUPER_ADMIN))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("DINTHIALMA_SUPER_ADMIN");
    assertThat(accessControl.estSuperAdmin(u)).isFalse();
  }
}
