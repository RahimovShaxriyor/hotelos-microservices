package com.hotelos.reception.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class HotelClockConfig {

    @Bean
    public Clock hotelClock(@Value("${hotelos.hotel-zone:Asia/Tashkent}") String hotelZone) {
        return Clock.system(ZoneId.of(hotelZone));
    }
}
