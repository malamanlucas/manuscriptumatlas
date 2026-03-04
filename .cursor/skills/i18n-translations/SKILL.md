---
name: i18n-translations
description: Internationalization and translation patterns. Use when working on i18n, tradução, locale, mensagens, idioma, or translate.
---

# i18n e Traduções

## Regra de ouro

NENHUM texto visível ao usuário hardcoded — sempre via chaves i18n (frontend) ou dados traduzidos do backend.

## Onde cada tradução vive

| Tipo | Onde | Exemplos |
|------|------|----------|
| **Backend** | API + tabelas `*_translations` | Nomes de concílios, heresias, pais da igreja, resumos IA, descrições |
| **Frontend** | `messages/{en,pt,es}.json` | Labels, botões, títulos de UI, placeholders, mensagens de erro |

Dados de domínio vêm da API com `?locale=`; labels de interface ficam nos arquivos de mensagens.

## Backend

- Endpoints com texto traduzível: aceitar `?locale=` (en, pt, es)
- JOINs com tabelas `*_translations` para retornar texto no locale solicitado
- Fallback para inglês quando tradução ausente

## Frontend

- `useTranslations("namespace")` do next-intl — apenas para strings de UI
- Atualizar SEMPRE os 3 arquivos: `messages/en.json`, `messages/pt.json`, `messages/es.json`
- Chaves aninhadas: `councils.confidenceLabel`, `ingestion.councilIngestion.phases.council_seed`

## Tradução por IA

- `BiographySummarizationService.translateCouncilFields()` — concílios
- `BiographySummarizationService.translateHeresyFields()` — heresias
- Fases: `council_translate_all`, `heresy_translate_all` no CouncilIngestionService

## Armadilha

`translationSource = "reviewed"` não deve ser sobrescrito por tradução machine. Verificar antes de persistir.

## Quick-fix: Nova chave i18n

Adicionar em cada um dos 3 arquivos, no mesmo path:

```json
"novaChave": "Texto em inglês"   // en.json
"novaChave": "Texto em português" // pt.json
"novaChave": "Texto em espanhol"  // es.json
```
