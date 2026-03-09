package com.example.paymentsystem.settlement.repository;

import com.example.paymentsystem.settlement.entity.Settlement;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  @Query("SELECT s.partnerId FROM Settlement s WHERE s.settlementDate = :settlementDate")
  Set<Long> findPartnerIdsBySettlementDate(@Param("settlementDate") LocalDateTime settlementDate);
}
