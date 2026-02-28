#!/bin/bash

# Script para instalar Docker no Ubuntu 24
echo "Iniciando instalação do Docker..."

# Atualizar pacotes
apt update
apt upgrade -y

# Instalar dependências
apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

# Adicionar chave GPG oficial do Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Adicionar repositório do Docker
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Atualizar índice de pacotes
apt update

# Instalar Docker Engine
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Iniciar e habilitar Docker
systemctl start docker
systemctl enable docker

# Verificar instalação
docker --version
docker compose version

# Adicionar usuário ao grupo docker (opcional)
# usermod -aG docker $USER

echo "Docker instalado com sucesso!"
echo "Para usar Docker sem sudo, execute: usermod -aG docker \$USER"
echo "Depois faça logout e login novamente."