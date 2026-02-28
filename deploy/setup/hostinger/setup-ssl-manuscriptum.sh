#!/bin/bash
set -e

# Configurar SSL + Nginx para vocemereceoinferno.com.br (Manuscriptum Atlas)
# Uso: bash setup-ssl-manuscriptum.sh seu-email@exemplo.com
# Executar no servidor de producao como root

DOMAIN="vocemereceoinferno.com.br"
WWW_DOMAIN="www.vocemereceoinferno.com.br"
FRONTEND_PORT="35855"
NGINX_CONF="/etc/nginx/sites-available/$DOMAIN"
NGINX_ENABLED="/etc/nginx/sites-enabled/$DOMAIN"
EMAIL="${1:-}"

if [ -z "$EMAIL" ]; then
    echo "Uso: bash $0 <email-para-letsencrypt>"
    echo "Exemplo: bash $0 meuemail@gmail.com"
    exit 1
fi

check_status() {
    if [ $? -eq 0 ]; then
        echo "[OK] $1"
    else
        echo "[ERRO] $1"
        exit 1
    fi
}

echo "=== SSL + Nginx: $DOMAIN ==="
echo "Frontend port: $FRONTEND_PORT"
echo "Email: $EMAIL"
echo

# 1. Instalar certbot se necessario
echo "=> Verificando certbot..."
if command -v certbot &> /dev/null; then
    echo "   certbot ja instalado: $(certbot --version 2>&1)"
else
    echo "   Instalando certbot + plugin nginx..."
    apt-get update -qq
    apt-get install -y -qq certbot python3-certbot-nginx
    check_status "Instalacao do certbot"
fi

# 2. Criar bloco nginx (HTTP apenas — certbot adicionara SSL)
echo ""
echo "=> Criando configuracao nginx..."
cat > "$NGINX_CONF" << 'NGINXEOF'
server {
    listen 80;
    listen [::]:80;
    server_name vocemereceoinferno.com.br www.vocemereceoinferno.com.br;

    location / {
        proxy_pass http://127.0.0.1:35855;

        proxy_busy_buffers_size   512k;
        proxy_buffers   4 512k;
        proxy_buffer_size   256k;

        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;

        add_header Cache-Control 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0';
        add_header Pragma "no-cache";
    }
}
NGINXEOF
check_status "Criacao do bloco nginx"

# 3. Habilitar site (symlink)
if [ ! -L "$NGINX_ENABLED" ]; then
    ln -s "$NGINX_CONF" "$NGINX_ENABLED"
fi
check_status "Symlink sites-enabled"

# 4. Testar e recarregar nginx
echo ""
echo "=> Testando configuracao nginx..."
nginx -t
check_status "Teste nginx"
systemctl reload nginx
check_status "Reload nginx"

# 5. Emitir certificado SSL via certbot
echo ""
echo "=> Emitindo certificado SSL para $DOMAIN e $WWW_DOMAIN..."
certbot --nginx \
    -d "$DOMAIN" \
    -d "$WWW_DOMAIN" \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    --redirect
check_status "Emissao do certificado SSL"

# 6. Reload final
systemctl reload nginx
check_status "Reload nginx final"

# 7. Verificar renovacao automatica
echo ""
echo "=> Verificando renovacao automatica..."
certbot renew --dry-run
check_status "Teste de renovacao automatica"

echo ""
echo "=== CONCLUIDO ==="
echo "Frontend: https://$DOMAIN"
echo "WWW:      https://$WWW_DOMAIN -> redireciona para https://$DOMAIN"
echo ""
echo "Arquivos:"
echo "  Nginx:        $NGINX_CONF"
echo "  Certificado:  /etc/letsencrypt/live/$DOMAIN/"
echo ""
echo "Comandos uteis:"
echo "  Testar nginx:       nginx -t && systemctl reload nginx"
echo "  Ver certificados:   certbot certificates"
echo "  Renovar manual:     certbot renew"
