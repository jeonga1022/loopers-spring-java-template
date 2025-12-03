package com.loopers.infrastructure.pg;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgFeignConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
            1000, TimeUnit.MILLISECONDS,  // connectTimeout: 1초
            3000, TimeUnit.MILLISECONDS,  // readTimeout: 3초
            true                           // followRedirects
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}