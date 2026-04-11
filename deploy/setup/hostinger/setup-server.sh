#!/bin/bash

# Script de automação para configuração completa do servidor
# Instala nginx, configura proxy reverso e gera certificados SSL

echo "=== INICIANDO CONFIGURAÇÃO AUTOMÁTICA DO SERVIDOR ==="
echo "Data: $(date)"
echo

# Variáveis de configuração
DOMAIN="degracarecebestes.com.br"
WWW_DOMAIN="www.degracarecebestes.com.br"
BACKEND_DOMAIN="backend.degracarecebestes.com.br"
PROXY_PORT="35852"  # Porta do frontend
BACKEND_PORT="35853"  # Porta do backend
EMAIL="malamanlucas@gmail.com"  # Email para certificados

echo "Configurações:"
echo "- Domínio: $DOMAIN"
echo "- WWW: $WWW_DOMAIN"
echo "- Backend: $BACKEND_DOMAIN"
echo "- Proxy frontend para: http://localhost:$PROXY_PORT"
echo "- Proxy backend para: http://localhost:$BACKEND_PORT"
echo "- Email: $EMAIL"
echo

# Função para verificar se comando foi executado com sucesso
check_status() {
    if [ $? -eq 0 ]; then
        echo "✅ $1 - Sucesso"
    else
        echo "❌ $1 - Erro"
        exit 1
    fi
}

# 1. Atualizar sistema
echo "📦 Atualizando sistema..."
apt update && apt upgrade -y
check_status "Atualização do sistema"

# 2. Instalar nginx
echo "🌐 Instalando nginx..."
apt install -y nginx
check_status "Instalação do nginx"

# 3. Remover configuração padrão
echo "🗑️ Removendo configuração padrão..."
rm -f /etc/nginx/sites-enabled/default
check_status "Remoção da configuração padrão"

# 4. Criar configuração do nginx
echo "⚙️ Criando configuração do nginx..."
cat > /etc/nginx/sites-available/$DOMAIN << EOF
# Frontend - Domínio principal e www
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN $WWW_DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:$PROXY_PORT;

        proxy_busy_buffers_size   512k;
        proxy_buffers   4 512k;
        proxy_buffer_size   256k;

        proxy_set_header Host \$http_host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Port \$server_port;

        add_header Cache-Control 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0';
        add_header Pragma "no-cache";
    }
}

# Backend - Subdomínio backend
server {
    listen 80;
    listen [::]:80;
    server_name $BACKEND_DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;

        proxy_busy_buffers_size   512k;
        proxy_buffers   4 512k;
        proxy_buffer_size   256k;

        proxy_set_header Host \$http_host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Port \$server_port;

        # Headers específicos para API
        add_header Access-Control-Allow-Origin "*" always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Origin, X-Requested-With, Content-Type, Accept, Authorization" always;
        
        # Responder a requisições OPTIONS
        if (\$request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
            add_header Access-Control-Allow-Headers "Origin, X-Requested-With, Content-Type, Accept, Authorization";
            add_header Content-Length 0;
            add_header Content-Type text/plain;
            return 204;
        }
    }
}
EOF
check_status "Criação da configuração do nginx"

# 5. Habilitar site
echo "🔗 Habilitando site..."
ln -s /etc/nginx/sites-available/$DOMAIN /etc/nginx/sites-enabled/
check_status "Habilitação do site"

# 6. Testar configuração do nginx
echo "🧪 Testando configuração do nginx..."
nginx -t
check_status "Teste da configuração do nginx"

# 7. Iniciar e habilitar nginx
echo "🚀 Iniciando nginx..."
systemctl start nginx
systemctl enable nginx
check_status "Inicialização do nginx"

# 8. Verificar status do nginx
echo "📊 Verificando status do nginx..."
systemctl status nginx --no-pager

# 9. Instalar certbot
echo "🔐 Instalando certbot..."
apt install -y certbot python3-certbot-nginx
check_status "Instalação do certbot"

# 10. Gerar certificados SSL
echo "🛡️ Gerando certificados SSL..."
echo "ATENÇÃO: Certifique-se de que os domínios $DOMAIN, $WWW_DOMAIN e $BACKEND_DOMAIN estão apontando para este servidor!"
echo "Pressione ENTER para continuar ou Ctrl+C para cancelar..."
read -r

# Gerar certificado automaticamente para todos os domínios
certbot --nginx -d $DOMAIN -d $WWW_DOMAIN -d $BACKEND_DOMAIN --non-interactive --agree-tos --email $EMAIL --redirect
check_status "Geração dos certificados SSL"

# 11. Reiniciar nginx
echo "🔄 Reiniciando nginx..."
systemctl restart nginx
check_status "Reinicialização do nginx"

# 12. Configurar renovação automática
echo "⏰ Configurando renovação automática..."
# Testar renovação
certbot renew --dry-run
check_status "Teste de renovação automática"

# 13. Mostrar status final
echo
echo "=== CONFIGURAÇÃO CONCLUÍDA COM SUCESSO! ==="
echo
echo "📋 Resumo:"
echo "✅ Nginx instalado e configurado"
echo "✅ Proxy reverso configurado para frontend: http://localhost:$PROXY_PORT"
echo "✅ Proxy reverso configurado para backend: http://localhost:$BACKEND_PORT"
echo "✅ Certificados SSL gerados para $DOMAIN, $WWW_DOMAIN e $BACKEND_DOMAIN"
echo "✅ Redirecionamento HTTP -> HTTPS ativo"
echo "✅ Renovação automática configurada"
echo "✅ CORS configurado para API backend"
echo
echo "🌐 URLs disponíveis:"
echo "Frontend:"
echo "- http://$DOMAIN (redireciona para HTTPS)"
echo "- https://$DOMAIN"
echo "- http://$WWW_DOMAIN (redireciona para HTTPS)"
echo "- https://$WWW_DOMAIN"
echo "Backend/API:"
echo "- http://$BACKEND_DOMAIN (redireciona para HTTPS)"
echo "- https://$BACKEND_DOMAIN"
echo
echo "📁 Arquivos importantes:"
echo "- Configuração: /etc/nginx/sites-available/$DOMAIN"
echo "- Certificados: /etc/letsencrypt/live/$DOMAIN/"
echo "- Logs nginx: /var/log/nginx/"
echo "- Logs certbot: /var/log/letsencrypt/"
echo
echo "🔧 Comandos úteis:"
echo "- Verificar status nginx: systemctl status nginx"
echo "- Recarregar nginx: systemctl reload nginx"
echo "- Testar configuração: nginx -t"
echo "- Renovar certificados: certbot renew"
echo "- Ver certificados: certbot certificates"
echo
echo "⚠️ IMPORTANTE:"
echo "- Certifique-se de que o frontend está rodando na porta $PROXY_PORT"
echo "- Certifique-se de que o backend está rodando na porta $BACKEND_PORT"
echo "- Os certificados serão renovados automaticamente"
echo "- CORS está configurado para permitir requisições do frontend para o backend"
echo "- Monitore os logs em caso de problemas"
echo
echo "🎉 Servidor configurado e pronto para uso!"
echo "Data de conclusão: $(date)"