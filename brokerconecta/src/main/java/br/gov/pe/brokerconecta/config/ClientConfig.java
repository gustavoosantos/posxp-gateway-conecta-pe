package br.gov.pe.brokerconecta.config;

import lombok.Data;

@Data
public class ClientConfig {
    private String xRoadId; 
    private String clientId;
    private String clientSecret;
}