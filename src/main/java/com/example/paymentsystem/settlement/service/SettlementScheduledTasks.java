package com.example.paymentsystem.settlement.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduledTasks {

  private final SettlementService settlementService;

  @Scheduled(cron = "0 * * * * ?")
  @SchedulerLock(name = "settlement_daily")
  public void dailySettlement() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    log.info("[일별 정산 시작] 대상 날짜: {}", yesterday);
    settlementService.settleForDate(yesterday);
  }
}
