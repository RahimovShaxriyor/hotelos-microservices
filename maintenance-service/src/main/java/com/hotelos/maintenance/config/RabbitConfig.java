package com.hotelos.maintenance.config;

import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = MessagingConstants.HOTEL_EXCHANGE;
    public static final String MAINTENANCE_ISSUE_UPDATED = RoutingKeys.MAINTENANCE_ISSUE_UPDATED;
    public static final String ROOM_ENGINEERING_CHANGED = RoutingKeys.ROOM_ENGINEERING_CHANGED;
    public static final String MAINTENANCE_CLEANUP_COMPLETED_QUEUE = "maintenance.cleanup.completed";

    @Bean
    public TopicExchange hotelExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public Queue maintenanceCleanupCompletedQueue() {
        return QueueBuilder.durable(MAINTENANCE_CLEANUP_COMPLETED_QUEUE).build();
    }

    @Bean
    public Binding maintenanceCleanupCompletedBinding(TopicExchange hotelExchange, Queue maintenanceCleanupCompletedQueue) {
        return BindingBuilder.bind(maintenanceCleanupCompletedQueue)
                .to(hotelExchange)
                .with(RoutingKeys.MAINTENANCE_CLEANUP_COMPLETED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
