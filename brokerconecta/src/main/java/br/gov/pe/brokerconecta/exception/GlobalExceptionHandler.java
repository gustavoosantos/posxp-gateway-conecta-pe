package br.gov.pe.brokerconecta.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handler para erros de configuração ou requisições mal formadas para o broker.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Erro de requisição inválida: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("erro_broker", ex.getMessage()));
    }

    /**
     * Handler para erros retornados pelas APIs externas (ex: 401, 404, 500 da API de destino).
     * Inclui tratamento especial para o erro 429 (Too Many Requests).
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<?> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("Erro retornado pela API externa. Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        
        // NOVO TRATAMENTO: Verifica se o erro é de limite de requisições excedido.
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Limite de requisições para a API externa foi excedido (Rate Limiting).");
            Map<String, String> errorBody = Map.of(
                "erro_servico_externo", "Limite de requisições por segundo excedido.",
                "detalhe", "O serviço de destino está sobrecarregado. Por favor, tente novamente mais tarde."
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorBody);
        }
        
        // Para todos os outros erros, repassa a resposta original da API externa.
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getResponseBodyAsString());
    }

    /**
     * Handler para erros de conexão, como TIMEOUT.
     */
    @ExceptionHandler({WebClientRequestException.class, TimeoutException.class})
    public ResponseEntity<Map<String, String>> handleConnectionErrors(Exception ex) {
        Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
        String errorMessage = String.format(
            "Não foi possível conectar ao serviço externo: %s. Verifique a conectividade com o host ou o status do serviço de destino.", 
            rootCause.getMessage()
        );
        
        log.error("Erro de conexão/timeout ao tentar acessar API externa. Causa: {}", rootCause.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of("erro_gateway", errorMessage));
    }

    /**
     * Handler para erros de segurança, como cliente não configurado ou sem permissão.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        log.warn("Falha de segurança/autorização: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403 Forbidden é o status mais apropriado
                .body(Map.of("erro_autorizacao", ex.getMessage()));
    }

    /**
     * Handler genérico para qualquer outra exceção não esperada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Ocorreu um erro inesperado e não tratado no broker", ex);
        return ResponseEntity
                .internalServerError()
                .body(Map.of("erro", "Ocorreu um erro inesperado no broker."));
    }
}
