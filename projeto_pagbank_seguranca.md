# Projeto PagBank — Arquitetura de Segurança

## Contexto

Trabalho na PagBank, em um projeto de força de vendas. Meu time é responsável pela segurança das aplicações.

## O que eu construí

Desenvolvi toda a arquitetura de autenticação e autorização do projeto. O coração disso é a emissão de tokens JWT com assinatura assimétrica — o Authorization Server detém a chave privada e emite os tokens; os Resource Servers possuem apenas a chave pública, conseguindo validar a assinatura sem nunca ter acesso à chave privada.

O JWT é autoassinado e carrega as permissões, as informações básicas do usuário, e trafega entre as aplicações de forma segura. Configurei todos os microsserviços de recursos para validarem o token automaticamente usando o Spring Security, que facilita muito esse processo.

Implementei também o mecanismo de introspecção de token, que permite revogar credenciais imediatamente em caso de qualquer incidente de segurança.

## SSO com Microsoft

Além disso, criei um microsserviço separado de autenticação integrado à Microsoft — o que chamei de "Alfa SSO". Ele usa OAuth 2.0 com Authorization Code Grant, obrigando o usuário a autenticar com o e-mail corporativo e validar via Microsoft Authenticator no dispositivo Android — MFA.

Após a autenticação bem-sucedida no Alfa SSO, a responsabilidade de gerar o JWT interno é delegada para outro microsserviço. Esse token interno é especial — diferente dos tokens dos recursos — e é ele que trafega pelas aplicações internas.

## Resultado

Com essa arquitetura, garanto que as credenciais do usuário nunca são expostas, os recursos estão protegidos, e em caso de problema podemos revogar o acesso imediatamente.
