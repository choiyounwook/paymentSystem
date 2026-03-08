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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduledTasks {

  private final PaymentRepository paymentRepository;
  private final SettlementRepository settlementRepository;

  @Scheduled(cron = "0 * * * * ?")
  @SchedulerLock(name = "settlement_daily")
  public void dailySettlement() {
    log.info("[일별 정산 시작]");
    LocalDate yesterday = LocalDate.now().minusDays(1);
    LocalDateTime startDate = yesterday.atStartOfDay();
    LocalDateTime endDate = yesterday.atTime(LocalTime.MAX);

    Map<Long, BigDecimal> settlementMap = getSettlementMap(startDate, endDate);
    processSettlements(settlementMap, yesterday);
  }

  private Map<Long, BigDecimal> getSettlementMap(LocalDateTime startDate, LocalDateTime endDate) {
    List<Payment> paymentList = paymentRepository.findByPaymentDateBetweenAndStatus(startDate,
        endDate, PaymentStatus.PAID);
    log.info("[일별 정산] {} ~ {} 총 결제 내역 {} 개", startDate, endDate, paymentList.size());
    return paymentList.stream().collect(Collectors.groupingBy(
        Payment::getPartner,
        Collectors.reducing(
            BigDecimal.ZERO,
            Payment::getAmount,
            BigDecimal::add
        )
    ));
  }

  private void processSettlements(Map<Long, BigDecimal> settlementMap,
      LocalDate settlementDate) {
    List<Settlement> settlements = settlementMap.entrySet().stream()
        .filter(entry -> !settlementRepository.existsByPartnerIdAndSettlementDate(
            entry.getKey(), settlementDate.atStartOfDay()))
        .map(entry -> Settlement.createCompletedSettlement(entry.getKey(), entry.getValue(),
            settlementDate))
        .toList();
    log.info("[일별 정산] 총 {}개 정산 진행 완료", settlements.size());
    settlementRepository.saveAll(settlements);
  }
}
