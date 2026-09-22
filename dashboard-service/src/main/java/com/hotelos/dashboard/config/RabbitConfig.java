package com.hotelos.dashboard.config;

import com.hotelos.common.event.MessagingConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = MessagingConstants.HOTEL_EXCHANGE;
    public static final String DASHBOARD_QUEUE = "dashboard.all.events";

    @Bean
    public TopicExchange hotelExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public Queue dashboardQueue() { return QueueBuilder.durable(DASHBOARD_QUEUE).build(); }

    @Bean
    public Binding allEventsBinding(TopicExchange hotelExchange, Queue dashboardQueue) {
        return BindingBuilder.bind(dashboardQueue).to(hotelExchange).with("#");
    }

    @Bean
    public MessageConverter messageConverter() { return new SimpleMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
