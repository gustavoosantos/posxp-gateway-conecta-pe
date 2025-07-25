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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenManagerService {

    private final WebClient.Builder webClientBuilder;
    private final CacheManager cacheManager;

    // 1. Cache temporário para requisições de token em andamento (in-flight).
    //    Usa ConcurrentHashMap para ser seguro em ambiente com múltiplas threads.
    private final Map<String, Mono<String>> inFlightRequests = new ConcurrentHashMap<>();

    public Mono<String> getAccessToken(ClientConfig clientConfig, ApiConfig apiConfig) {
        
        Cache longTermCache = cacheManager.getCache("tokens");
        String cacheKey = clientConfig.getClientId();
        
        // 2. Tenta buscar no cache de longo prazo primeiro (Caffeine).
        Cache.ValueWrapper valueWrapper = (longTermCache != null) ? longTermCache.get(cacheKey) : null;
        if (valueWrapper != null) {
            log.info(">>> [CACHE HIT] Token encontrado no cache de longo prazo para o cliente: {}", cacheKey);
            return Mono.just((String) valueWrapper.get());
        }

        // 3. Se não encontrou, usa o cache in-flight para lidar com a concorrência.
        //    computeIfAbsent é atômico: garante que o bloco de código só será executado UMA VEZ
        //    pela primeira thread que chegar aqui para uma dada chave.
        return inFlightRequests.computeIfAbsent(cacheKey, key -> {
            log.warn(">>> [CONCURRENCY] Cache de longo prazo vazio. Iniciando busca de novo token para o cliente: {}", key);
            
            // 4. Apenas a primeira thread executa esta parte.
            return fetchTokenFromProvider(clientConfig, apiConfig)
                    .doOnSuccess(token -> {
                        // 5. Ao obter o token com sucesso, armazena no cache de longo prazo.
                        if (longTermCache != null && token != null) {
                            log.info(">>> [CACHE] Armazenando novo token no cache de longo prazo: {}", key);
                            longTermCache.put(key, token);
                        }
                    })
                    // 6. Remove a requisição do mapa in-flight quando ela termina (com sucesso ou erro).
                    .doOnTerminate(() -> {
                        log.debug(">>> [CONCURRENCY] Finalizando requisição in-flight para o cliente: {}", key);
                        inFlightRequests.remove(key);
                    })
                    // 7. O operador .cache() é a chave! Ele garante que a chamada WebClient
                    //    seja executada apenas uma vez, e seu resultado é "reproduzido" para
                    //    todas as threads concorrentes que estavam "escutando" este Mono.
                    .cache(); 
        });
    }

    /**
     * Método que realmente executa a chamada WebClient para o provedor de identidade.
     */
    private Mono<String> fetchTokenFromProvider(ClientConfig clientConfig, ApiConfig apiConfig) {
        log.info(">>> [API CALL] Disparando chamada WebClient para obter token...");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

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
            .doOnError(error -> log.error("Erro ao obter token para cliente {}: {}", clientConfig.getClientId(), error.getMessage()));
    }
}
