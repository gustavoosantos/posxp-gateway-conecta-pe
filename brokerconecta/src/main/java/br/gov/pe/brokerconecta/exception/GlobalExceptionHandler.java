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
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("Erro retornado pela API externa. Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getResponseBodyAsString());
    }

    /**
     * NOVO HANDLER: Captura erros de conexão, como TIMEOUT.
     * Lançado quando o WebClient não consegue nem mesmo se conectar ao servidor de destino.
     */
    @ExceptionHandler({WebClientRequestException.class, TimeoutException.class})
    public ResponseEntity<Map<String, String>> handleConnectionErrors(Exception ex) {
        // A WebClientRequestException geralmente encapsula a causa raiz (ex: ConnectTimeoutException)
        Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
        String errorMessage = String.format(
            "Não foi possível conectar ao serviço externo: %s. Verifique a conectividade com o host ou o status do serviço de destino.", 
            rootCause.getMessage()
        );
        
        log.error("Erro de conexão/timeout ao tentar acessar API externa. Causa: {}", rootCause.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT) // 504 Gateway Timeout é o status HTTP mais apropriado aqui.
                .body(Map.of("erro_gateway", errorMessage));
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