package com.hotelos.roomservice.config;

import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import org.springframework.amqp.core.TopicExchange;
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
    public static final String ROOM_SERVICE_ORDER_UPDATED = RoutingKeys.ROOM_SERVICE_ORDER_UPDATED;
    public static final String ROOM_SERVICE_CHARGE = RoutingKeys.ROOM_SERVICE_CHARGE;

    @Bean
    public TopicExchange hotelExchange() { return new TopicExchange(EXCHANGE, true, false); }

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
