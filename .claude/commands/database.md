Você está trabalhando no banco PostgreSQL/Flyway do Manuscriptum Atlas. Siga estas regras:

## ALERTA CRÍTICO
Flyway NÃO executa migrations em runtime. O fallback em `FlywayConfig.kt` cria o schema. Migrations `.sql` são apenas documentação.

## Nova tabela (normal)
1. Criar `V{N}__descricao.sql` (referência) — verificar última versão em `backend/src/main/resources/db/migration/`
2. Criar objeto `Table` em `Tables.kt`
3. **Registrar em `SchemaUtils.createMissingTablesAndColumns()` em `FlywayConfig.kt`** — sem isso: `PSQLException: relation "xxx" does not exist`
4. Índices extras em `applyExtraIndexesAndConstraints()`

## Adicionar coluna
1. Criar migration SQL (referência)
2. Adicionar coluna no objeto `Table` em `Tables.kt`
3. SchemaUtils detecta colunas novas automaticamente

## Tabelas particionadas
SchemaUtils NÃO suporta particionamento. Usar `exec(SQL)` raw no fallback. Ver `createVisitorPartitionedTables()` como exemplo.

Analise o pedido do usuário: $ARGUMENTS
