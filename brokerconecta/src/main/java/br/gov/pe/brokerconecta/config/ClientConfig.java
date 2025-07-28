package br.gov.pe.brokerconecta.config;

import lombok.Data;
import java.util.Map;

@Data
public class ClientConfig {
    private String xRoadId;
    private Map<String, ApiPermissionConfig> authorizedApis;
}