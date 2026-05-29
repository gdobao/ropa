package com.colorinchi.app.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient aiWebClient(AiServerProperties properties) {
        Duration connectTimeout = properties.connectTimeout() == null ? Duration.ofSeconds(5) : properties.connectTimeout();
        Duration readTimeout = properties.readTimeout() == null ? Duration.ofSeconds(20) : properties.readTimeout();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(readTimeout);

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.apiKey())) {
                        headers.setBearerAuth(properties.apiKey());
                    }
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
