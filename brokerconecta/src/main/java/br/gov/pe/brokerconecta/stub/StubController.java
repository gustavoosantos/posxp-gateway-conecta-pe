package br.gov.pe.brokerconecta.stub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Profile("stub") // Este Controller só existe quando o perfil "stub" está ativo
public class StubController {

    /**
     * Simula o endpoint de obtenção de token.
     * Usamos MultiValueMap para simular o recebimento de dados de formulário.
     */
    @PostMapping(value = "/stubs/conecta/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> getStubToken(@RequestParam MultiValueMap<String, String> formParams) {
        log.info(">>> [STUB] Gerando token de acesso falso para o client_id: {}", formParams.getFirst("client_id"));
        
        Map<String, Object> fakeTokenResponse = Map.of(
            "access_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdHViLWNsaWVudCIsIm5hbWUiOiJCcm9rZXIgU3R1YiIsImlhdCI6MTc1MzEyMTUzNCwiZXhwIjoxNzUzMTI4NzM0fQ.fake-signature-for-poc",
            "expires_in", 7200,
            "token_type", "Bearer"
        );
        
        return ResponseEntity.ok(fakeTokenResponse);
    }

    /**
     * Simula o endpoint de consulta de CPF com a resposta detalhada.
     */
    @PostMapping("/stubs/conecta/cpf")
    public ResponseEntity<List<Map<String, Object>>> getStubCpfData(
            @RequestHeader Map<String, String> headers,
            @RequestBody JsonNode requestBody) {
        
        log.info(">>> [STUB] Endpoint de consulta de CPF (detalhado) ativado!");
        log.debug(">>> [STUB] Header x-cpf-usuario: {}", headers.get("x-cpf-usuario"));
        log.debug(">>> [STUB] Corpo da requisição: {}", requestBody.toString());

        List<String> cpfsParaConsultar = new ArrayList<>();
        if (requestBody.has("listaCpf")) {
            for (JsonNode cpfNode : requestBody.get("listaCpf")) {
                cpfsParaConsultar.add(cpfNode.asText());
            }
        }

        // Para cada CPF na lista, cria um registro falso detalhado
        List<Map<String, Object>> responseList = cpfsParaConsultar.stream()
                .map(this::createFakeCpfRecord)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Método auxiliar para criar um registro de CPF falso com a estrutura completa.
     * @param cpf O CPF para o qual o registro será criado.
     * @return Um mapa representando o objeto JSON de resposta.
     */
    private Map<String, Object> createFakeCpfRecord(String cpf) {
        return Map.ofEntries(
            Map.entry("CPF", cpf),
            Map.entry("Nome", "NOME COMPLETO FAKE DE " + cpf.substring(0, 3)),
            Map.entry("SituacaoCadastral", 0),
            Map.entry("ResidenteExterior", "N"),
            Map.entry("NomeMae", "NOME DA MAE FAKE DE " + cpf.substring(0, 3)),
            Map.entry("DataNascimento", "19850115"),
            Map.entry("Sexo", 1),
            Map.entry("NaturezaOcupacao", 2),
            Map.entry("NomeNaturezaOcupacao", "2022 - Empregado de instituições financeiras públicas e privadas"),
            Map.entry("OcupacaoPrincipal", 120),
            Map.entry("NomeOcupacaoPrincipal", "Dirigente, presidente e diretor de empresa industrial, comercial ou prestadora de serviços"),
            Map.entry("ExercicioOcupacao", 2023),
            Map.entry("TipoLogradouro", "AVENIDA"),
            Map.entry("Logradouro", "AVENIDA PRINCIPAL DO STUB"),
            Map.entry("NumeroLogradouro", "1234"),
            Map.entry("Complemento", "APT 101"),
            Map.entry("Bairro", "BAIRRO FAKE"),
            Map.entry("Cep", 50000000),
            Map.entry("UF", "PE"),
            Map.entry("CodigoMunicipio", 2611606),
            Map.entry("Municipio", "RECIFE"),
            Map.entry("DDD", 81),
            Map.entry("UnidadeAdministrativa", 418000),
            Map.entry("NomeUnidadeAdministrativa", "DRF RECIFE"),
            Map.entry("Estrangeiro", "N"),
            Map.entry("DataAtualizacao", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))),
            Map.entry("CodigoMunicipioNaturalidade", 2611606),
            Map.entry("NomeMunicipioNaturalidade", "RECIFE"),
            Map.entry("UFMunicipioNaturalidade", "PE")
        );
    }
}