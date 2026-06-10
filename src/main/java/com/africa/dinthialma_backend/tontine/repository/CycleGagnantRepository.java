package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleGagnantRepository extends JpaRepository<CycleGagnant, UUID> {

  List<CycleGagnant> findByCycle_IdAndDeletedAtIsNull(UUID cycleId);

  boolean existsByCycle_IdAndDeletedAtIsNull(UUID cycleId);
}
