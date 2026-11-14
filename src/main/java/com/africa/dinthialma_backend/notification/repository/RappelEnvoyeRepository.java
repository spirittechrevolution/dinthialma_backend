package com.africa.dinthialma_backend.notification.repository;

import com.africa.dinthialma_backend.notification.entity.RappelEnvoye;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RappelEnvoyeRepository extends JpaRepository<RappelEnvoye, UUID> {}
