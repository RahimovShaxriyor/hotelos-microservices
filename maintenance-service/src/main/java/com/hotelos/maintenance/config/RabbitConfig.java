package com.hotelos.maintenance.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "hotel.exchange";
    public static final String MAINTENANCE_ISSUE_UPDATED = "maintenance.issue.updated";
    public static final String ROOM_STATUS_CHANGED = "room.status.changed";

    @Bean
    public TopicExchange hotelExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
