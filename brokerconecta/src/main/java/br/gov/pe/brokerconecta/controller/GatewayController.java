package br.gov.pe.brokerconecta.controller;

import br.gov.pe.brokerconecta.config.ApiConfig;
import br.gov.pe.brokerconecta.config.BrokerProperties;
import br.gov.pe.brokerconecta.config.ClientConfig;
import br.gov.pe.brokerconecta.config.RouteConfig;
import br.gov.pe.brokerconecta.service.TokenManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
public class GatewayController {

    private final BrokerProperties brokerProperties;
    private final TokenManagerService tokenManagerService;
    private final WebClient.Builder webClientBuilder;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardRequest(
            HttpMethod method,
            HttpServletRequest request,
            @RequestHeader("X-Road-Client") String xRoadClient) throws IOException {

        String requestPath = request.getRequestURI();
        log.info("Requisição recebida do cliente [{}]: {} {}", xRoadClient, method, requestPath);

        // 1. Encontra a rota correspondente na configuração (application.yml)
        RouteConfig route = brokerProperties.getRoutes().stream()
                .filter(r -> r.getMethod().name().equalsIgnoreCase(method.name()) && pathMatcher.match(r.getPath(), requestPath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rota configurada para o caminho: " + requestPath));

        // 2. Extrai variáveis do path, se houver (ex: /path/{id})
        Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(route.getPath(), requestPath);
        
        // 3. Busca a configuração do cliente e da API de destino
        ClientConfig clientConfig = findClientConfig(xRoadClient);
        ApiConfig apiConfig = findApiConfig(route.getApi());

        // 4. Lê o corpo da requisição (essencial para POST)
        byte[] requestBody = request.getInputStream().readAllBytes();

        // 5. Inicia o fluxo reativo: primeiro, busca o token de acesso
        return tokenManagerService.getAccessToken(clientConfig, apiConfig)
            .flatMap(token -> {
                // 6. Com o token em mãos, monta a URL de destino
                URI targetUri = UriComponentsBuilder.fromUriString(apiConfig.getTargetUrl())
                        .buildAndExpand(pathVariables)
                        .toUri();

                log.info("Roteamento ID [{}]. Encaminhando para: {}", route.getId(), targetUri);

                // 7. Configura a chamada WebClient para o serviço de destino
                WebClient.RequestBodySpec requestBodySpec = webClientBuilder.build()
                        .method(method)
                        .uri(targetUri)
                        .headers(httpHeaders -> {
                            // 8. Copia todos os headers da requisição original (incluindo x-cpf-usuario)
                            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                                if (!headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                                    httpHeaders.add(headerName, request.getHeader(headerName));
                                }
                            });
                            // Adiciona o header de autorização com o token obtido
                            httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        });

                WebClient.RequestHeadersSpec<?> spec;
                if (requestBody.length > 0) {
                    // 9. Adiciona o corpo da requisição, se existir
                    spec = requestBodySpec.body(BodyInserters.fromValue(requestBody));
                } else {
                    spec = requestBodySpec;
                }
                
                // 10. Executa a chamada e retorna a resposta
                return spec.retrieve().toEntity(byte[].class);
            })
            // 11. Log de erro aprimorado para depuração
            .doOnError(error -> {
                log.error("### ERRO INESPERADO NA CADEIA DO GATEWAY ###");
                log.error("Tipo de Erro: {}", error.getClass().getName());
                log.error("Mensagem: {}", error.getMessage());
            });
    }

    /**
     * Busca a configuração do cliente com base no header X-Road-Client,
     * comparando com o campo 'xRoadId' no application.yml.
     */
    private ClientConfig findClientConfig(String xRoadClientHeader) {
        return brokerProperties.getClients().values().stream()
                .filter(config -> xRoadClientHeader.equals(config.getXRoadId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cliente não configurado para o X-Road ID: " + xRoadClientHeader));
    }

    /**
     * Busca a configuração da API de destino com base no nome fornecido na rota.
     */
    private ApiConfig findApiConfig(String apiName) {
        return Optional.ofNullable(brokerProperties.getApis().get(apiName))
                .orElseThrow(() -> new IllegalArgumentException("API '" + apiName + "' não configurada."));
    }
}