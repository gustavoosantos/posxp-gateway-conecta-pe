package br.gov.pe.brokerconecta.service;

import br.gov.pe.brokerconecta.config.ApiConfig;
import br.gov.pe.brokerconecta.config.ClientConfig;
import br.gov.pe.brokerconecta.dto.TokenResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * Serviço responsável por gerenciar o ciclo de vida dos tokens de acesso.
 * Utiliza um cache manual para funcionar corretamente com o fluxo reativo (Mono).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenManagerService {

    private final WebClient.Builder webClientBuilder;
    private final CacheManager cacheManager;

    /**
     * Ponto de entrada principal para obter um token de acesso.
     * Verifica primeiro o cache e, em caso de falha (miss), busca um novo token.
     *
     * @param clientConfig Configuração do cliente que está fazendo a requisição.
     * @param apiConfig    Configuração da API de destino, que contém a URL do token.
     * @return Um Mono contendo o token de acesso (String).
     */
    public Mono<String> getAccessToken(ClientConfig clientConfig, ApiConfig apiConfig) {
        
        Cache cache = cacheManager.getCache("tokens");
        if (cache == null) {
            // Fallback de segurança caso o cache não seja encontrado
            return fetchAndCacheToken(clientConfig, apiConfig, null);
        }

        String cacheKey = clientConfig.getClientId();
        Cache.ValueWrapper valueWrapper = cache.get(cacheKey);

        // CACHE HIT: O token foi encontrado no cache.
        if (valueWrapper != null) {
            log.info(">>> [CACHE HIT] Token encontrado no cache para o cliente: {}", cacheKey);
            return Mono.just((String) valueWrapper.get());
        }

        // CACHE MISS: O token não foi encontrado, precisa buscar um novo.
        return fetchAndCacheToken(clientConfig, apiConfig, cache);
    }

    /**
     * Método privado que executa a chamada WebClient para o servidor de autenticação
     * e armazena o resultado no cache em caso de sucesso.
     */
    private Mono<String> fetchAndCacheToken(ClientConfig clientConfig, ApiConfig apiConfig, Cache cache) {
        String cacheKey = clientConfig.getClientId();
        log.info(">>> [CACHE MISS] Solicitando novo token de acesso para o cliente: {}", cacheKey);

        // O corpo da requisição agora contém apenas o grant_type
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        // As credenciais são combinadas, codificadas em Base64 e enviadas no header Authorization
        String auth = clientConfig.getClientId() + ":" + clientConfig.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        String authHeader = "Basic " + encodedAuth;

        return webClientBuilder.build()
            .post()
            .uri(apiConfig.getTokenUrl())
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(TokenResponseDTO.class)
            .map(TokenResponseDTO::getAccessToken)
            .doOnSuccess(token -> {
                // Em caso de sucesso, o novo token é armazenado no cache
                if (cache != null && token != null) {
                    log.info(">>> [CACHE] Armazenando novo token no cache para o cliente: {}", cacheKey);
                    cache.put(cacheKey, token);
                }
            })
            .doOnError(error -> log.error("Erro ao obter token para cliente {}: {}", clientConfig.getClientId(), error.getMessage()));
    }
}