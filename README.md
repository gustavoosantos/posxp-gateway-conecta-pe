# posxp-gateway-conecta-pe

Breve PoC de API Gateway estadual para unificar acesso às APIs do Conecta.gov.br.

# Broker Conecta

## Visão Geral

Este projeto é um Módulo de Integração (Broker Conecta.PE) para centralizar e simplificar o acesso às APIs da plataforma Conecta.gov.br, abstraindo a complexidade de autenticação e gerenciamento de tokens para as aplicações consumidoras dos órgãos estaduais. O módulo atua como um intermediário que encapsula toda a lógica de autenticação, geração de token e roteamento de chamadas. Assim, as secretarias poderão acessar, de forma unificada e simplificada, os serviços federais de CPF, CNPJ e demais APIs do Conecta.gov.br, sem precisar se preocupar com detalhes de implementação e conformidade.

O foco inicial desta Prova de Conceito (PoC) é a integração com a API de consulta de CPF.

## Tecnologias utilizadas
- Java JDK 21 
- Apache Maven 3.9+
- Spring Boot 3
- Eclipse IDE
- Postman (ou similar) para testes OAuth2

## Requisitos
- Java JDK 21 
- Apache Maven 3.9+
- Acesso às credenciais (Client ID/Secret) do ambiente de sandbox do Conecta.gov.br

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

