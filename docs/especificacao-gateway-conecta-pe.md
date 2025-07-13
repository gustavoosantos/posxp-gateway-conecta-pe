# Documento de Especificação: Módulo de Integração - API Gateway Estadual (Broker Conecta.PE)

## 1. Introdução

### 1.1. Propósito

O presente documento descreve a análise de requisitos para o desenvolvimento de um Módulo de Integração, doravante denominado "Broker Conecta.PE". Este sistema atuará como um intermediário (proxy/broker) entre as aplicações dos órgãos estaduais e as APIs disponibilizadas pelo Governo Federal através da plataforma Conecta.gov.br.

O objetivo principal é abstrair e centralizar a complexidade do processo de autenticação e gerenciamento de tokens de acesso, simplificando o consumo de dados federais para as aplicações estaduais e garantindo um ponto central de controle e auditoria através da integração com a plataforma Xroad.

### 1.2. Escopo

O Broker Conecta será responsável por:

- Receber requisições de aplicações estaduais destinadas às APIs federais.
- Identificar a aplicação cliente.
- Gerenciar o ciclo de vida dos tokens de autenticação (obtenção e renovação).
- Mapear e utilizar as chaves de acesso corretas para cada API federal.
- Repassar a requisição à API de destino.

**Não está no escopo deste projeto o desenvolvimento das aplicações clientes ou das APIs federais.**

**Nota:** A aplicação será desenvolvida com foco em uso genérico para múltiplas APIs federais. No entanto, para fins de testes e validações da Prova de Conceito (PoC), o escopo será direcionado especificamente à integração com a **API CPF Light (versão 2.5.0)**, fornecida via Conecta.

A documentação técnica dessa API está disponível no catálogo oficial do Conecta em:
https://www.gov.br/conecta/catalogo/apis/cadastro-base-do-cidadao-cbc-cpf/swagger.json/swagger_view

## 2. Requisitos Funcionais

| ID | Requisito | Descrição | Prioridade |
| --- | --- | --- | --- |
| RF-001 | Recepção de Requisições | O sistema deve expor um endpoint para receber requisições HTTP/S das aplicações clientes. | Alta |
| RF-002 | Identificação do Cliente | O sistema deve identificar a aplicação cliente que origina a requisição através de um cabeçalho HTTP específico, como X-Road-Client, que será injetado pelo Servidor Seguro do Xroad. | Alta |
| RF-003 | Mapeamento de Credenciais | Com base no identificador do cliente (RF-002), o sistema deve consultar um arquivo de configuração para obter as credenciais necessárias (ex: Client ID, Client Secret) e o endpoint da API federal de destino. | Alta |
| RF-004 | Gerenciamento de Cache de Token | O sistema deve manter um cache em memória para armazenar os tokens de acesso para cada combinação de cliente/API. | Alta |
| RF-005 | Geração de Token de Acesso | Se um token válido não existir no cache (RF-004) ou estiver expirado, o sistema deve realizar uma chamada ao endpoint de token do SSO federal (seguindo o fluxo Client Credentials) para gerar um novo token. O novo token e seu tempo de expiração devem ser armazenados no cache. | Alta |
| RF-006 | Encaminhamento de Requisição | O sistema deve adicionar o token de acesso válido ao cabeçalho de autorização da requisição original e encaminhá-la para a API federal correspondente. | Alta |
| RF-007 | Configuração Dinâmica | O sistema deve permitir que o mapeamento de clientes, chaves e APIs seja gerenciado através de um arquivo de configuração externo, sem a necessidade de reimplantar a aplicação a cada nova configuração. | Média |

## 3. Requisitos Não Funcionais

| ID | Requisito | Descrição |
| --- | --- | --- |
| RNF-001 | Desempenho | A aplicação deve ser de alta performance, com baixa latência, capaz de suportar múltiplas conexões concorrentes. |
| RNF-002 | Escalabilidade | A arquitetura deve ser stateless e permitir a execução em múltiplas instâncias (réplicas) de forma horizontal para garantir alta disponibilidade e distribuição de carga. |
| RNF-003 | Manutenibilidade | O código-fonte deve ser limpo, bem documentado e de fácil manutenção.  |

## 4. Arquitetura Proposta

- **Linguagem/Plataforma**: Java (versão 21 ou superior para aproveitar recursos como Virtual Threads).
- **Framework**: Spring Boot será utilizado como base do projeto, utilizando recursos para alto desempenho.
- **Arquitetura Cloud Native**: A aplicação deve seguir os princípios de arquitetura cloud native, sendo stateless, com configuração externa, escalável horizontalmente e pronta para ambientes de alta disponibilidade.
- **Gerenciamento de Configuração**: O sistema utilizará arquivos externos de configuração (YAML, JSON ou .properties), compatíveis com ambientes conteinerizados. Esses arquivos poderão ser montados como volumes ou gerenciados como secrets/config maps em plataformas de conteinerização, mas sem que isso represente uma obrigatoriedade — a aplicação será flexível quanto à fonte e ao modo de injeção das configurações.
- **Armazenamento Temporário (Cache)**: A aplicação utilizará inicialmente o **Caffeine** como mecanismo de cache local em memória para armazenamento de tokens e dados temporários. Caso haja tempo disponível durante a execução do projeto, será realizada a implantação do cache com **Redis**, permitindo cache distribuído e maior escalabilidade (desejável).

- **Diagrama de Fluxo Simplificado**:

*(Inserir diagrama aqui)*

## 5. Próximos Passos e Prova de Conceito (PoC)

- **Desenvolver Piloto**: Implementar uma versão mínima do Broker Conecta utilizando Spring Boot.
- **Simular Ambiente**: Em ambiente local (Podman), simular a chamada do Xroad (injetando o header X-Road-Client) e os endpoints das APIs federais e do SSO.
- **Definir Estrutura de Configuração**: Estruturar e validar os arquivos de configuração externa (YAML, JSON ou `.properties`), assegurando compatibilidade com ambientes conteinerizados.
- **Apresentar Resultados**: Realizar testes e documentar os resultados.
