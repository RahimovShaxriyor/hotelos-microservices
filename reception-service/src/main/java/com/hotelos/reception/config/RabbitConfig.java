package com.hotelos.reception.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "hotel.exchange";
    public static final String ROOM_VACATED = "room.vacated";
    public static final String ROOM_STATUS_CHANGED = "room.status.changed";
    public static final String ROOM_SERVICE_CHARGE = "room.service.charge";

    public static final String RECEPTION_ROOM_STATUS_QUEUE = "reception.room.status.changed";
    public static final String RECEPTION_CHARGE_QUEUE = "reception.room.service.charge";

    @Bean
    public TopicExchange hotelExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue receptionRoomStatusQueue() {
        return QueueBuilder.durable(RECEPTION_ROOM_STATUS_QUEUE).build();
    }

    @Bean
    public Queue receptionChargeQueue() {
        return QueueBuilder.durable(RECEPTION_CHARGE_QUEUE).build();
    }

    @Bean
    public Binding roomStatusBinding(TopicExchange hotelExchange, @Qualifier("receptionRoomStatusQueue") Queue receptionRoomStatusQueue) {
        return BindingBuilder.bind(receptionRoomStatusQueue).to(hotelExchange).with(ROOM_STATUS_CHANGED);
    }

    @Bean
    public Binding chargeBinding(TopicExchange hotelExchange, @Qualifier("receptionChargeQueue") Queue receptionChargeQueue) {
        return BindingBuilder.bind(receptionChargeQueue).to(hotelExchange).with(ROOM_SERVICE_CHARGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
