---
name: database-postgres
description: Quick-fixes and pitfalls for PostgreSQL schema and Flyway. Use when working on banco, tabela, migration, schema, PostgreSQL, Flyway, or coluna.
---

# Database PostgreSQL — Quick Fixes

Complementa `database-migrations.mdc`. Para convenções completas, ver essa rule.

## ALERTA CRÍTICO

**Flyway NÃO executa migrations** — o sistema usa fallback em `FlywayConfig.kt`. A migration `.sql` é apenas documentação. Toda estrutura deve ser criada no fallback Kotlin. Se algo só existir no `.sql`, não será criado e dará erro em runtime.

## Quick-fix: Nova tabela (normal)

1. Criar `V{N}__descricao.sql` (referência) — próxima: **V17**
2. Criar objeto `Table` em `Tables.kt`
3. Registrar em `SchemaUtils.createMissingTablesAndColumns()` em `FlywayConfig.kt`
4. Índices extras em `applyExtraIndexesAndConstraints()` se necessário

## Quick-fix: Adicionar coluna a tabela existente

1. Criar migration SQL (referência)
2. Adicionar coluna no objeto `Table` em `Tables.kt`
3. `SchemaUtils.createMissingTablesAndColumns()` detecta colunas novas automaticamente

## Armadilha: Tabelas particionadas

SchemaUtils NÃO suporta particionamento. Usar `exec(SQL)` raw no fallback (ex: `createVisitorPartitionedTables()`). Ver `database-migrations.mdc` para checklist completo.
