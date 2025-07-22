# posxp-gateway-conecta-pe

Breve PoC de API Gateway estadual para unificar acesso às APIs do Conecta.gov.br.

# Broker Conecta

## Visão Geral

Este projeto é um Módulo de Integração (Broker) desenvolvido em **Spring Boot 3** e **Java 21**. Ele atua como um intermediário (API Gateway) para centralizar e simplificar o acesso às APIs da plataforma Conecta.gov.br, abstraindo a complexidade de autenticação e gerenciamento de tokens para as aplicações consumidoras.

O foco inicial desta Prova de Conceito (PoC) é a integração com a API de consulta de CPF.

## Requisitos
- Java JDK 21 
- Apache Maven 3.9+
- Spring Boot 3
- Acesso às credenciais (Client ID/Secret) do ambiente de sandbox do Conecta.gov.br
- Eclipse IDE
- Git
- Postman (ou similar) para testes OAuth2

## Estrutura
- `/brokerconecta/src/main/java` – Código-fonte do microserviço
- `/docs` – Planejamento, especificações e lições aprendidas

---

## Configuração

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/seu-usuario/broker-conecta.git](https://github.com/seu-usuario/broker-conecta.git)
    cd broker-conecta
    ```

2.  **Configure as credenciais:**
    - Abra o arquivo `src/main/resources/application.yml`.
    - Localize a seção `broker.clients`.
    - Substitua os valores de `clientId` e `clientSecret` pelas suas credenciais reais do Conecta.gov.br.

---

## Como Construir e Executar

### 1. Construir o Projeto

Use o Maven Wrapper para garantir a consistência do build. Da raiz do projeto, execute:

```bash
./mvnw clean install

