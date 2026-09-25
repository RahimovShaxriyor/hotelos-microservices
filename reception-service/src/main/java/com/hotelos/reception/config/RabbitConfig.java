package com.hotelos.reception.config;

import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String RECEPTION_HOUSEKEEPING_STATUS_QUEUE = "reception.housekeeping.room.status.changed";
    public static final String RECEPTION_ENGINEERING_STATUS_QUEUE = "reception.maintenance.room.status.changed";
    public static final String RECEPTION_CHARGE_QUEUE = "reception.room.service.charge";

    @Bean
    public TopicExchange hotelExchange() {
        return new TopicExchange(MessagingConstants.HOTEL_EXCHANGE, true, false);
    }

    @Bean
    public Queue receptionHousekeepingStatusQueue() {
        return QueueBuilder.durable(RECEPTION_HOUSEKEEPING_STATUS_QUEUE).build();
    }

    @Bean
    public Queue receptionEngineeringStatusQueue() {
        return QueueBuilder.durable(RECEPTION_ENGINEERING_STATUS_QUEUE).build();
    }

    @Bean
    public Queue receptionChargeQueue() {
        return QueueBuilder.durable(RECEPTION_CHARGE_QUEUE).build();
    }

    @Bean
    public Binding housekeepingStatusBinding(TopicExchange hotelExchange, @Qualifier("receptionHousekeepingStatusQueue") Queue receptionHousekeepingStatusQueue) {
        return BindingBuilder.bind(receptionHousekeepingStatusQueue).to(hotelExchange).with(RoutingKeys.ROOM_HOUSEKEEPING_CHANGED);
    }

    @Bean
    public Binding engineeringStatusBinding(TopicExchange hotelExchange, @Qualifier("receptionEngineeringStatusQueue") Queue receptionEngineeringStatusQueue) {
        return BindingBuilder.bind(receptionEngineeringStatusQueue).to(hotelExchange).with(RoutingKeys.ROOM_ENGINEERING_CHANGED);
    }

    @Bean
    public Binding chargeBinding(TopicExchange hotelExchange, @Qualifier("receptionChargeQueue") Queue receptionChargeQueue) {
        return BindingBuilder.bind(receptionChargeQueue).to(hotelExchange).with(RoutingKeys.ROOM_SERVICE_CHARGE);
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
