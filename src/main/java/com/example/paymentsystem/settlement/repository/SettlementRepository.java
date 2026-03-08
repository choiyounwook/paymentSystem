package com.example.paymentsystem.settlement.repository;

import com.example.paymentsystem.settlement.entity.Settlement;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  boolean existsByPartnerIdAndSettlementDate(Long partnerId, LocalDateTime settlementDate);
}
