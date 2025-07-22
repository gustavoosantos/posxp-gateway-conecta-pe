package br.gov.pe.brokerconecta.config;

import lombok.Data;
import org.springframework.http.HttpMethod;

@Data
public class RouteConfig {
    private String id;
    private String path;
    private HttpMethod method;
    private String api;
}