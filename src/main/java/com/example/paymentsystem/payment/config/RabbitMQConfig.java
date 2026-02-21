package com.example.paymentsystem.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String PAYMENT_CANCEL_EXCHANGE = "payment-cancel-exchange";
  public static final String PAYMENT_CANCEL_QUEUE = "payment-cancel-queue";
  public static final String PAYMENT_CANCEL_ROUTING_KEY = "payment-cancel-routing-key";
  public static final String PAYMENT_CANCEL_DLQ = "payment-cancel-dlq";
  public static final String PAYMENT_CANCEL_DLX = "payment-cancel-dlx";
  private static final String PAYMENT_CANCEL_DLQ_ROUTING_KEY = "payment-cancel-dlq-routing-key";

  @Bean
  public Queue paymentCancelQueue() {
    return QueueBuilder.durable(PAYMENT_CANCEL_QUEUE)
        .withArgument("x-dead-letter-exchange", PAYMENT_CANCEL_DLX)
        .withArgument("x-dead-letter-routing-key", PAYMENT_CANCEL_DLQ_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue paymentCancelDlq() {
    return QueueBuilder.durable(PAYMENT_CANCEL_DLQ).build();
  }

  @Bean
  public DirectExchange paymentCancelExchange() {
    return new DirectExchange(PAYMENT_CANCEL_EXCHANGE);
  }

  @Bean
  public DirectExchange paymentCancelDlx() {
    return new DirectExchange(PAYMENT_CANCEL_DLX);
  }

  @Bean
  public Binding paymentCancelBinding() {
    return BindingBuilder.bind(paymentCancelQueue()).to(paymentCancelExchange())
        .with(PAYMENT_CANCEL_ROUTING_KEY);
  }

  @Bean
  public Binding paymentCancelDlqBinding() {
    return BindingBuilder.bind(paymentCancelDlq()).to(paymentCancelDlx())
        .with(PAYMENT_CANCEL_DLQ_ROUTING_KEY);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
