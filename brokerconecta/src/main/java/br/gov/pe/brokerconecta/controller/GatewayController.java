package br.gov.pe.brokerconecta.controller;

import br.gov.pe.brokerconecta.config.ApiConfig;
import br.gov.pe.brokerconecta.config.ApiPermissionConfig;
import br.gov.pe.brokerconecta.config.BrokerProperties;
import br.gov.pe.brokerconecta.config.ClientConfig;
import br.gov.pe.brokerconecta.config.RouteConfig;
import br.gov.pe.brokerconecta.service.TokenManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Controller principal que atua como um API Gateway.
 * Intercepta todas as requisições, gerencia a autorização, autenticação
 * e encaminha para o serviço de destino apropriado.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Gateway de Acesso", description = "Endpoints principais do Broker Conecta")
public class GatewayController {

    private final BrokerProperties brokerProperties;
    private final TokenManagerService tokenManagerService;
    private final WebClient.Builder webClientBuilder;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @RequestMapping("/api/v1/**")
    @Operation(
        summary = "Proxy para APIs do Conecta.gov.br",
        description = "Este é um endpoint genérico que atua como proxy, roteando a requisição para a API de destino correta com base no caminho (path) e no método HTTP, após gerenciar a autenticação.",
        requestBody = @RequestBody(
            description = "Corpo da requisição a ser encaminhado para a API de destino.",
            required = false, // O corpo pode não ser necessário para requisições GET
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Object.class),
                examples = {
                    @ExampleObject(
                        name = "Exemplo Consulta de CPF",
                        summary = "Exemplo para consulta de CPF em lote",
                        value = "{\"listaCpf\":[\"11122233344\", \"55566677788\"]}"
                    )
                }
            )
        )
    )
    @Parameter(name = "X-Road-Client", in = ParameterIn.HEADER, required = true, description = "Identificador único do sistema cliente. Ex: SAD/PORTAL_CIDADAO")
    @Parameter(name = "x-cpf-usuario", in = ParameterIn.HEADER, description = "CPF do usuário final que está realizando a operação.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Requisição bem-sucedida", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: header ausente)", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autorizado (ex: token inválido)", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado (cliente não tem permissão para a API)", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro interno na API de destino", content = @Content),
        @ApiResponse(responseCode = "504", description = "Timeout na comunicação com a API de destino", content = @Content)
    })
    public Mono<ResponseEntity<byte[]>> forwardRequest(
            @Parameter(hidden = true) HttpMethod method,
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestHeader("X-Road-Client") String xRoadClientHeader) throws IOException {

        String requestPath = request.getRequestURI();
        log.info("Requisição recebida do cliente [{}]: {} {}", xRoadClientHeader, method, requestPath);

        // 1. Identifica a rota para saber qual API está sendo solicitada
        RouteConfig route = findRoute(method, requestPath);
        String apiName = route.getApi();

        // 2. Busca a configuração do cliente com base no header
        ClientConfig clientConfig = findClientConfigByXRoadId(xRoadClientHeader);

        // 3. Busca a permissão específica para a API dentro da configuração do cliente
        ApiPermissionConfig permission = findApiPermission(clientConfig, apiName);
        
        // 4. O resto do fluxo usa as credenciais da permissão encontrada
        ApiConfig apiConfig = findApiConfig(apiName);
        Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(route.getPath(), requestPath);
        byte[] requestBody = request.getInputStream().readAllBytes();

        // Passa as credenciais específicas para o serviço de token
        return tokenManagerService.getAccessToken(permission, apiConfig)
            .flatMap(token -> {
                URI targetUri = UriComponentsBuilder.fromUriString(apiConfig.getTargetUrl())
                        .buildAndExpand(pathVariables)
                        .toUri();

                log.info("Roteamento ID [{}]. Encaminhando para: {}", route.getId(), targetUri);

                WebClient.RequestBodySpec requestBodySpec = webClientBuilder.build()
                        .method(method)
                        .uri(targetUri)
                        .headers(httpHeaders -> {
                            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                                if (!headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                                    httpHeaders.add(headerName, request.getHeader(headerName));
                                }
                            });
                            httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        });

                WebClient.RequestHeadersSpec<?> spec;
                if (requestBody.length > 0) {
                    spec = requestBodySpec.body(BodyInserters.fromValue(requestBody));
                } else {
                    spec = requestBodySpec;
                }
                
                return spec.retrieve().toEntity(byte[].class);
            })
            .doOnError(error -> {
                log.error("### ERRO INESPERADO NA CADEIA DO GATEWAY ###");
                log.error("Tipo de Erro: {}", error.getClass().getName());
                log.error("Mensagem: {}", error.getMessage());
            });
    }

    /**
     * Encontra a rota correspondente no application.yml com base no método e caminho da requisição.
     */
    private RouteConfig findRoute(HttpMethod method, String requestPath) {
        return brokerProperties.getRoutes().stream()
                .filter(r -> r.getMethod() == method && pathMatcher.match(r.getPath(), requestPath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rota configurada para o caminho: " + requestPath));
    }

    /**
     * Encontra a configuração do cliente com base no header X-Road-Client.
     */
    private ClientConfig findClientConfigByXRoadId(String xRoadClientHeader) {
        return brokerProperties.getClients().values().stream()
                .filter(config -> xRoadClientHeader.equals(config.getXRoadId()))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Cliente com X-Road ID '" + xRoadClientHeader + "' não está configurado."));
    }
    
    /**
     * Encontra a permissão (e as credenciais) para uma API específica dentro da configuração de um cliente.
     */
    private ApiPermissionConfig findApiPermission(ClientConfig clientConfig, String apiName) {
        return Optional.ofNullable(clientConfig.getAuthorizedApis().get(apiName))
                .orElseThrow(() -> new SecurityException("Acesso negado. O cliente não tem permissão para acessar a API '" + apiName + "'."));
    }

    /**
     * Encontra a configuração da API de destino com base no nome.
     */
    private ApiConfig findApiConfig(String apiName) {
        return Optional.ofNullable(brokerProperties.getApis().get(apiName))
                .orElseThrow(() -> new IllegalArgumentException("API '" + apiName + "' não configurada."));
    }
}