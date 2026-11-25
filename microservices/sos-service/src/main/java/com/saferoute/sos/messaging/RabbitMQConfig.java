package com.saferoute.sos.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ (Communication Asynchrone - S7).
 * Activée lorsque `saferoute.messaging.rabbitmq.enabled=true` (ex: profil docker).
 */
@Configuration
@ConditionalOnProperty(name = "saferoute.messaging.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    public static final String SOS_EXCHANGE = "sos.events.exchange";
    public static final String SOS_TRIGGERED_QUEUE = "sos.triggered.queue";
    public static final String SOS_STATUS_QUEUE = "sos.status.changed.queue";
    public static final String SOS_LOCATION_QUEUE = "sos.location.updated.queue";

    public static final String ROUTING_KEY_TRIGGERED = "sos.alert.triggered";
    public static final String ROUTING_KEY_STATUS = "sos.alert.status.changed";
    public static final String ROUTING_KEY_LOCATION = "sos.alert.location.updated";

    @Bean
    public TopicExchange sosEventsExchange() {
        return new TopicExchange(SOS_EXCHANGE, true, false);
    }

    @Bean
    public Queue sosTriggeredQueue() {
        return new Queue(SOS_TRIGGERED_QUEUE, true);
    }

    @Bean
    public Queue sosStatusQueue() {
        return new Queue(SOS_STATUS_QUEUE, true);
    }

    @Bean
    public Queue sosLocationQueue() {
        return new Queue(SOS_LOCATION_QUEUE, true);
    }

    @Bean
    public Binding bindSosTriggered(Queue sosTriggeredQueue, TopicExchange sosEventsExchange) {
        return BindingBuilder.bind(sosTriggeredQueue).to(sosEventsExchange).with(ROUTING_KEY_TRIGGERED);
    }

    @Bean
    public Binding bindSosStatus(Queue sosStatusQueue, TopicExchange sosEventsExchange) {
        return BindingBuilder.bind(sosStatusQueue).to(sosEventsExchange).with(ROUTING_KEY_STATUS);
    }

    @Bean
    public Binding bindSosLocation(Queue sosLocationQueue, TopicExchange sosEventsExchange) {
        return BindingBuilder.bind(sosLocationQueue).to(sosEventsExchange).with(ROUTING_KEY_LOCATION);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
