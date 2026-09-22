package com.hotelos.housekeeping.config;

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
    public static final String ROOM_VACATED = RoutingKeys.ROOM_VACATED;
    public static final String ROOM_HOUSEKEEPING_CHANGED = RoutingKeys.ROOM_HOUSEKEEPING_CHANGED;
    public static final String HOUSEKEEPING_ROOM_VACATED_QUEUE = "housekeeping.room.vacated";

    @Bean
    public TopicExchange hotelExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public Queue roomVacatedQueue() { return QueueBuilder.durable(HOUSEKEEPING_ROOM_VACATED_QUEUE).build(); }

    @Bean
    public Binding roomVacatedBinding(TopicExchange hotelExchange, Queue roomVacatedQueue) {
        return BindingBuilder.bind(roomVacatedQueue).to(hotelExchange).with(ROOM_VACATED);
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
        return template;
    }
}
