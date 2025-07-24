package br.gov.pe.brokerconecta.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("tokens");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Token do Conecta expira em 2h (120 min). Usamos um cache de 110 min.
                .expireAfterWrite(110, TimeUnit.MINUTES)
                .maximumSize(500)); // Armazena no máximo 500 tokens diferentes
        return cacheManager;
    }
}
