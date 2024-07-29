package org.altervista.mangampire.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.altervista.mangampire.dto.ClientCart;
import org.altervista.mangampire.dto.EndpointRequest;
import org.altervista.mangampire.dto.RequestLogin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.TreeMap;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() { return new ObjectMapper(); }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    @Bean
    @Scope("prototype")
    public EndpointRequest endpointRequest() {
        return new EndpointRequest();
    }
    @Bean
    public RequestLogin loginCached() { return new RequestLogin(); }
    @Bean
    public Map<String, ClientCart> clientCartCached() { return new TreeMap<>(); }
    @Bean
    public Boolean sessionLogged() {
        return false;
    }

}
