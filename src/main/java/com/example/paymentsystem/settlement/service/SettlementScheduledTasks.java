package com.example.paymentsystem.settlement.service;

import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.repository.PaymentRepository;
import com.example.paymentsystem.settlement.entity.Settlement;
import com.example.paymentsystem.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduledTasks {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    @Scheduled(cron = "0 * * * * ?")
    public void dailySettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startDate = yesterday.atStartOfDay();
        LocalDateTime endDate = yesterday.atTime(LocalTime.MAX);
    }

    private Map<Long, BigDecimal> getSettlementMap(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> paymentList = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        return paymentList.stream().collect(Collectors.groupingBy(
                Payment::getPartner,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        Payment::getAmount,
                        BigDecimal::add
                )
        ));
    }

    private void processSettlements(Map<Long, BigDecimal> settlementMap, LocalDateTime paymentDate) {
        settlementMap.entrySet().stream().forEach(entry -> {
            Settlement settlement = Settlement.createCompletedSettlement(entry.getKey(), entry.getValue(), paymentDate);
            settlementRepository.save(settlement);
        });
    }
}
