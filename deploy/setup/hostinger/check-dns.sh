#!/bin/bash

DOMAIN="vocemereceoinferno.com.br"

echo "========================================="
echo "🔎 Verificando domínio: $DOMAIN"
echo "========================================="
echo ""

echo "1️⃣ IP(s) resolvido(s):"
dig +short $DOMAIN A
echo ""

echo "2️⃣ Nameservers autoritativos:"
dig +short NS $DOMAIN
echo ""

echo "3️⃣ Trace completo de resolução DNS:"
dig +trace $DOMAIN
echo ""

echo "4️⃣ Header HTTP (qual servidor está respondendo):"
curl -I http://$DOMAIN 2>/dev/null | grep -i "server"
echo ""

echo "5️⃣ Whois do IP principal:"
IP=$(dig +short $DOMAIN | head -n 1)
whois $IP | grep -E "OrgName|org-name|owner|inetnum|netname"
echo ""

echo "========================================="
echo "✅ Fim da verificação"
echo "========================================="