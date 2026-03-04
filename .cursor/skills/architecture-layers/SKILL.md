---
name: architecture-layers
description: Layered architecture and design decisions. Use when working on arquitetura, nova feature, novo domínio, camadas, or design.
---

# Arquitetura em Camadas

## Fluxo

```
Routes → Service → Repository → Database (Exposed/PostgreSQL)
```

Cada camada conhece apenas a imediatamente abaixo. Routes chamam Service; Service chama Repository; Repository usa Exposed/PostgreSQL.

## Decisões de design

**Novo Service vs. estender existente?** — Criar novo quando o domínio for distinto (ex: CouncilService vs. ChurchFatherService). Estender quando for variação do mesmo domínio.

**Novo Repository vs. adicionar método?** — Adicionar método quando operar sobre as mesmas tabelas. Criar novo quando houver entidade/tabela nova.

## Checklist completo de novo domínio

Ver `kotlin-conventions.mdc` seção "Registrar novos domínios" — 10 passos (Tables, DTOs, Repository, Service, Routes, Application.kt, FlywayConfig, migration SQL, seed, frontend).
