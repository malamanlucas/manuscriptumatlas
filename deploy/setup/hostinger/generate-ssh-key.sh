#!/bin/bash

# Script para gerar chave SSH e exibir no terminal
echo "=== Gerador de Chave SSH ==="
echo

# Solicitar informações do usuário
read -p "Digite seu email para a chave SSH: " email
read -p "Digite o nome do arquivo da chave (padrão: id_rsa): " key_name

# Usar nome padrão se não fornecido
if [ -z "$key_name" ]; then
    key_name="id_rsa"
fi

# Definir caminho completo da chave
key_path="$HOME/.ssh/$key_name"

echo
echo "Gerando chave SSH..."
echo "Arquivo: $key_path"
echo "Email: $email"
echo

# Criar diretório .ssh se não existir
mkdir -p ~/.ssh

# Gerar a chave SSH
ssh-keygen -t rsa -b 4096 -C "$email" -f "$key_path" -N ""

echo
echo "=== CHAVE SSH GERADA COM SUCESSO! ==="
echo
echo "Arquivos criados:"
echo "- Chave privada: $key_path"
echo "- Chave pública: $key_path.pub"
echo
echo "=== CHAVE PÚBLICA (copie e cole onde necessário) ==="
echo
cat "$key_path.pub"
echo
echo "=== INSTRUÇÕES ==="
echo "1. Copie a chave pública acima"
echo "2. Cole no servidor de destino em ~/.ssh/authorized_keys"
echo "3. Ou adicione em serviços como GitHub, GitLab, etc."
echo
echo "Para conectar usando esta chave:"
echo "ssh -i $key_path usuario@servidor"
echo
echo "=== FIM ==="