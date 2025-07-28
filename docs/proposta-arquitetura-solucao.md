# Proposta de Arquitetura de Solução

## Introdução

Este documento detalha a arquitetura de solução proposta para o **Broker Conecta**, projetada para garantir escalabilidade, alta disponibilidade e segurança. A arquitetura se baseia no uso do “barramento de dados” open-source **X-Road** e é implementada sobre os componentes da Amazon Web Services (AWS), aproveitando serviços gerenciados e componentes nativos selecionados por sua robustez, integração e suporte a modelos de alta disponibilidade.

## A Solução X-Road

O **X‑Road** é uma solução open-source de “barramento de dados” (Data Exchange Layer) que permite a troca segura e padronizada de informações entre sistemas heterogêneos, por meio de conexões criptografadas e certificados digitais. Sua função central na arquitetura é atuar como um middleware distribuído, garantindo confidencialidade, integridade e interoperabilidade das mensagens trocadas entre organizações.

### Componentes Principais do X-Road

- **Serviços Centrais (Central Services):** Repositório de registro de membros, políticas de segurança (autoridades de certificação, time-stamping) e configuração global que os Security Servers consomem periodicamente.
- **Servidores Seguros (Security Servers):** Pontos de extremidade instalados em cada organização, responsáveis por assinar, criptografar, autenticar e encaminhar as requisições SOAP/REST entre provedores e consumidores, sem armazenar dados permanentemente.
- **Sistemas de Informação (Information Systems):** As aplicações clientes e servidores que produzem ou consomem serviços por meio do X‑Road.

### Funcionamento Resumido

Em resumo, o X‑Road funciona como um “barramento de mensagens” federado, onde os **Security Servers** protegem e roteiam o tráfego entre sistemas, enquanto os **Central Services** mantêm a confiança e a configuração global do ecossistema.

> Nota: Detalhes de implementação ou do funcionamento interno do X-Road não serão abordados neste trabalho. Para fins de modelagem, serão representados apenas os Servidores Seguros e a conexão entre eles, tornando implícita a existência do componente Central Services.
> 

## Arquitetura da Solução na AWS

A implementação da solução será realizada utilizando os seguintes componentes principais da Amazon Web Services:

- **Application Load Balancer (ALB):** Distribui as requisições HTTP/HTTPS entre as instâncias EC2 de aplicação que estão no Target Group. É responsável também por realizar *health checks* e por escutar nas portas 80/443.
- **Auto Scaling Group (ASG):** Gerencia um grupo de instâncias EC2 para ajuste dinâmico da capacidade computacional conforme a carga. A configuração prevê de 3 a 6 instâncias distribuídas em sub-redes privadas, escalando automaticamente com base em métricas de CPU e tráfego de rede.
- **Instâncias EC2:** As máquinas virtuais que executarão a aplicação do Broker, distribuídas em múltiplas zonas de disponibilidade (AZ) para garantir tolerância a falhas.
- **Sub-redes e Conectividade:**
    - **Public Subnets:** Três sub-redes públicas (uma por AZ) contendo os **NAT Gateways** com Elastic IPs associados.
    - **Private Subnets:** Três sub-redes privadas para alocar as instâncias EC2 gerenciadas pelo Auto Scaling Group.
    - **Route Tables:** Configurações de rota para direcionar todo o tráfego de saída das sub-redes privadas através do NAT Gateway correspondente em sua AZ.
- **Security Groups:**
    - **SG-App:** Firewall virtual que permite a entrada de tráfego vindo exclusivamente do Application Load Balancer (nas portas 80/443).

## Elaboração do Diagrama Arquitetural

O diagrama da arquitetura foi elaborado na ferramenta Draw.io, utilizando os ícones oficiais da AWS para representar os principais componentes da solução, suas interações e a distribuição em três zonas de disponibilidade.

Foram modeladas as instâncias EC2 em um Auto Scaling Group, distribuídas em sub-redes privadas e balanceadas por um Application Load Balancer. Recursos como NAT Gateway e Security Groups foram incluídos para ilustrar a conectividade e a segurança, garantindo uma visão clara e completa da arquitetura proposta.
