package org.altervista.mangampire.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.altervista.mangampire.dto.ClientCart;
import org.altervista.mangampire.dto.EndpointRequest;
import org.altervista.mangampire.dto.RequestLogin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

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
    public ClientCart clientCartCached() { return new ClientCart(); }
    @Bean
    public Boolean sessionLogged() {
        return false;
    }

}
