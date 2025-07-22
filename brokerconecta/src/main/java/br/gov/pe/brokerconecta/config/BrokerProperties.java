package br.gov.pe.brokerconecta.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List; // Importar List
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerProperties {
    private List<RouteConfig> routes; 
    private Map<String, ApiConfig> apis;
    private Map<String, ClientConfig> clients;
}