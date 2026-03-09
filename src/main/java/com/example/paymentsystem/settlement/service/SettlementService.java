package com.example.paymentsystem.settlement.service;

import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.entity.PaymentStatus;
import com.example.paymentsystem.payment.repository.PaymentRepository;
import com.example.paymentsystem.settlement.entity.Settlement;
import com.example.paymentsystem.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

  private final PaymentRepository paymentRepository;
  private final SettlementRepository settlementRepository;

  @Transactional
  public void settleForDate(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.atTime(LocalTime.MAX);

    Map<Long, BigDecimal> settlementMap = groupByPartner(startDate, endDate);
    saveSettlements(settlementMap, date);
  }

  private Map<Long, BigDecimal> groupByPartner(LocalDateTime startDate, LocalDateTime endDate) {
    List<Payment> payments = paymentRepository.findByPaymentDateBetweenAndStatus(
        startDate, endDate, PaymentStatus.PAID);
    log.info("[일별 정산] {} ~ {} 총 결제 내역 {} 개", startDate, endDate, payments.size());

    return payments.stream().collect(Collectors.groupingBy(
        Payment::getPartner,
        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
    ));
  }

  private void saveSettlements(Map<Long, BigDecimal> settlementMap, LocalDate settlementDate) {
    Set<Long> alreadySettled = settlementRepository.findPartnerIdsBySettlementDate(
        settlementDate.atStartOfDay());

    List<Settlement> settlements = settlementMap.entrySet().stream()
        .filter(entry -> !alreadySettled.contains(entry.getKey()))
        .map(entry -> Settlement.createCompletedSettlement(
            entry.getKey(), entry.getValue(), settlementDate))
        .toList();

    log.info("[일별 정산] 총 {}개 정산 진행 완료", settlements.size());
    settlementRepository.saveAll(settlements);
  }
}
