package com.example.paymentsystem.payment.service;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SlackWebhookService {

    // slack url
    @Value("${slack.webhook.url}")
    private String webhookUrl;

    // slack 서비스를 이용하기 위해서 최초 인스턴스 생성
    private final Slack slack = Slack.getInstance();

    @Async
    public void sendToSlack(String text) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("Slack webhook URL is not configured. Skipping message: {}", text);
            return;
        }
        // slack으로 전달할 메시지 구성
        Payload payload = Payload.builder().text(text).build();
        try {
            // 메시지 전송
            slack.send(webhookUrl, payload);
            log.debug("Slack message sent successfully: {}", text);
        } catch (Exception e) {
            log.error("Slack webhook send failed: {}", text, e);
        }
    }
}
