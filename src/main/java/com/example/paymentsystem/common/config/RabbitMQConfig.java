package com.example.paymentsystem.common.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

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
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      MessageConverter messageConverter,
      MessageRecoverer messageRecoverer) {

    Map<Class<? extends Throwable>, Boolean> retryableMap = new HashMap<>();
    retryableMap.put(AmqpRejectAndDontRequeueException.class, false);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableMap, true, true);

    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(1000);

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(RetryInterceptorBuilder.stateless()
        .retryOperations(retryTemplate)
        .recoverer(messageRecoverer)
        .build());

    return factory;
  }

  @Bean
  public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
    return new RepublishMessageRecoverer(rabbitTemplate, PAYMENT_CANCEL_DLX,
        PAYMENT_CANCEL_DLQ_ROUTING_KEY);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
