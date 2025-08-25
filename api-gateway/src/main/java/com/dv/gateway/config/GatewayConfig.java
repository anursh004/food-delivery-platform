package com.dv.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

  @Bean
  public WebClient orderClient(@Value("${services.order.baseUri:http://order-service:8082}") String base){
    return WebClient.builder().baseUrl(base).build();
  }

  @Bean
  public WebClient catalogClient(@Value("${services.catalog.baseUri:http://catalog-service:8083}") String base){
    return WebClient.builder().baseUrl(base).build();
  }

  @Bean
  public WebClient logisticsClient(@Value("${services.logistics.baseUri:http://logistics-service:8084}") String base){
    return WebClient.builder().baseUrl(base).build();
  }
}
