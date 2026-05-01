# Code Review: `GET /api/reports/customer-usage`

**Endpoint:** `GET /api/reports/customer-usage`  
**Stack:** TypeScript + Prisma ORM + PostgreSQL  
**Context:** Multi-tenant analytics endpoint — account managers view API usage metrics for their customers.

---

## Part 1: Issue Identification

### Bloqueadores (impedem merge)

| # | Tipo | Localização | Descrição |
|---|------|-------------|-----------|
| B1 | Security | `req.params.accountId` | IDOR — sem verificação de que o usuário autenticado pertence à conta solicitada |
| B2 | Performance | loop principal | N+1 queries: 3 queries sequenciais por cliente → ~3.000 queries para 1.000 clientes |

### Bugs de corretude

| # | Tipo | Localização | Descrição |
|---|------|-------------|-----------|
| C1 | Correctness | `successRate`, `avgResponseTimeMs` | Divisão por zero quando `apiCalls.length === 0` → resultado `NaN` no JSON |
| C2 | Correctness | `checkIfOverLimit` | Re-executa `db.apiCall.count` para dados já disponíveis em `apiCalls.length` |

### Follow-up (não bloqueiam, mas devem ser endereçados)

| # | Tipo | Localização | Descrição |
|---|------|-------------|-----------|
| F1 | Performance | `Buffer.byteLength(call.requestBody)` | Carrega todos os bodies em memória para calcular tamanho — deve ser agregado no banco |
| F2 | Performance | `db.customer.findMany` sem limit | Sem paginação; resposta irrestrita com 1.000+ clientes |
| F3 | Reliability | função inteira | Sem `try/catch` — falha de DB retorna 500 não estruturado |
| F4 | Style | `customerIds` string concat | `O(n)` concatenações desnecessárias; `customers.map(c => c.id).join(',')` é suficiente |

---

## Part 2: Root Cause Analysis

### B1 — IDOR (Insecure Direct Object Reference)

O `accountId` é lido diretamente de `req.params` e usado como filtro no banco sem validar se o usuário da sessão tem acesso àquela conta. Em um sistema multi-tenant, qualquer usuário autenticado pode enumerar `accountId`s e acessar dados de outras organizações.

**Fix:** Antes de qualquer query, verificar que `req.user.accountId === accountId` (ou equivalente conforme o modelo de auth). Retornar `403` caso contrário.

### B2 — N+1 Queries

O loop itera sobre `customers` e, para cada cliente, executa:

1. `db.apiCall.findMany(...)` — todas as chamadas dos últimos 30 dias
2. `db.subscription.findFirst(...)` — assinatura atual
3. `db.apiCall.count(...)` dentro de `checkIfOverLimit` — recontagem redundante

Para N clientes, isso resulta em `3N` round-trips sequenciais ao banco. Com 1.000 clientes, são ~3.000 queries; com 10.000, ~30.000. É a causa direta do timeout em staging.

**Fix:** Substituir o loop por queries agregadas:

```sql
-- Substitui apiCall.findMany + contagens + data transfer
SELECT
  customer_id,
  COUNT(*) AS total_calls,
  COUNT(*) FILTER (WHERE status_code BETWEEN 200 AND 299) AS success_count,
  AVG(response_time_ms) AS avg_response_time,
  SUM(LENGTH(request_body) + LENGTH(response_body)) AS total_bytes
FROM api_calls
WHERE account_id = $1
  AND created_at >= NOW() - INTERVAL '30 days'
GROUP BY customer_id;

-- Substitui subscription.findFirst por cliente
SELECT customer_id, tier, monthly_limit
FROM subscriptions
WHERE customer_id = ANY($1);  -- array de IDs
```

Isso reduz de `3N` queries para **2 queries totais**, independente do número de clientes.

### C1 — Divisão por zero

Quando `apiCalls.length === 0` (cliente sem chamadas no período):

```ts
successRate: (successCount / 0) * 100  // → NaN
avgResponseTimeMs: 0 / 0               // → NaN
```

`NaN` é serializado como `null` em `JSON.stringify`, corrompendo os dados do relatório.

**Fix:**
```ts
successRate: apiCalls.length > 0 ? (successCount / apiCalls.length) * 100 : 0,
avgResponseTimeMs: apiCalls.length > 0 ? totalResponseTime / apiCalls.length : 0,
```

### C2 — `checkIfOverLimit` duplica trabalho

A função recebe `subscription` mas ignora `apiCalls` já carregados, executando uma nova query `db.apiCall.count`. O resultado é numericamente idêntico a `apiCalls.length` (mesma janela de 30 dias).

**Fix:** Eliminar `checkIfOverLimit` e calcular inline:

```ts
isOverLimit: subscription ? apiCalls.length > subscription.monthlyLimit : false,
```

---

## Part 3: Prioritized Fix Plan

### Fase 1 — Bloquear merge (fazer agora)

1. **Auth check no início do handler:**
   ```ts
   if (req.user.accountId !== accountId) {
     return res.status(403).json({ error: 'Forbidden' });
   }
   ```

2. **Substituir loop N+1 por queries agregadas** (ver SQL acima). Buscar clientes, métricas e subscriptions em 3 queries paralelas com `Promise.all`, depois fazer join em memória.

3. **Guardar zero-division:**
   ```ts
   const total = apiCalls.length;
   successRate: total > 0 ? (successCount / total) * 100 : 0,
   avgResponseTimeMs: total > 0 ? totalResponseTime / total : 0,
   ```

### Fase 2 — Follow-up (PR separado)

4. **Persistir `request_size_bytes` e `response_size_bytes` na escrita** de `apiCall` — elimina `Buffer.byteLength` em leitura.

5. **Adicionar paginação** com `cursor` ou `limit`/`offset` no endpoint.

6. **Envolver em `try/catch`** com resposta padronizada de erro.

---

## Diagrama: Fluxo Atual vs. Fluxo Corrigido

### Fluxo atual — problema N+1 + IDOR

```
GET /api/reports/customer-usage/:accountId
        │
        ▼
┌─────────────────────────────────┐
│  req.params.accountId           │  ← ⚠ SEM verificação de auth
│  (qualquer usuário acessa)      │     qualquer accountId funciona
└─────────────────────────────────┘
        │
        ▼
  db.customer.findMany()           ── Query 1 (ex: 1.000 clientes)
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  FOR EACH customer  (loop sequencial, N iterações)  │
│                                                     │
│  ├─ db.apiCall.findMany(customerId)  ── Query 2     │
│  │   └─ carrega TODOS os bodies em memória          │
│  │       para calcular Buffer.byteLength            │
│  │                                                  │
│  ├─ db.subscription.findFirst(customerId) ── Query 3│
│  │                                                  │
│  └─ checkIfOverLimit(customerId)                    │
│      └─ db.apiCall.count(customerId) ── Query 4     │
│          (duplica Query 2, mesma janela 30 dias)    │
│                                                     │
│  ⚠ Se apiCalls.length === 0:                        │
│    successRate = NaN, avgResponseTime = NaN         │
└─────────────────────────────────────────────────────┘
        │
        │  1.000 clientes × 3 queries = 3.000 queries sequenciais
        ▼
  TIMEOUT em staging ──────────────────────────────────────────
```

### Fluxo corrigido — 3 queries paralelas

```
GET /api/reports/customer-usage/:accountId
        │
        ▼
┌──────────────────────────────────────┐
│  req.user.accountId === accountId?   │
│  NÃO → 403 Forbidden                 │  ← auth check
│  SIM → continua                      │
└──────────────────────────────────────┘
        │
        ▼
  Promise.all([                         ── 3 queries em paralelo
    db.customer.findMany(accountId),    ── Query A: clientes
    db.apiCall.groupBy(accountId,30d),  ── Query B: métricas agregadas
    db.subscription.findMany(ids),      ── Query C: assinaturas
  ])
        │
        ▼
┌──────────────────────────────────────────────┐
│  JOIN em memória (O(n), sem I/O adicional)   │
│                                              │
│  Para cada cliente:                          │
│  ├─ successRate: total > 0 ? x/total : 0    │  ← sem NaN
│  ├─ avgResponseTime: total > 0 ? avg : 0    │  ← sem NaN
│  └─ isOverLimit: count > monthlyLimit        │  ← sem query extra
└──────────────────────────────────────────────┘
        │
        ▼
  < 500ms para 1.000+ clientes
```

### Mapa de issues por severidade

```
┌─────────────────────────────────────────────────────────────┐
│                    BLOQUEIA O MERGE                         │
├──────────────────────────┬──────────────────────────────────┤
│  B1 — IDOR               │  B2 — N+1 Queries               │
│  ─────────────────────   │  ───────────────────────────     │
│  Qualquer usuário acessa │  3.000 queries sequenciais       │
│  dados de qualquer conta │  → timeout com 1.000+ clientes   │
│  → vazamento multi-tenant│  → causa direta do bug staging   │
└──────────────────────────┴──────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    BUGS DE CORRETUDE                        │
├──────────────────────────┬──────────────────────────────────┤
│  C1 — Divisão por zero   │  C2 — Query duplicada           │
│  ─────────────────────   │  ───────────────────────────     │
│  Clientes sem chamadas   │  checkIfOverLimit refaz count    │
│  retornam NaN no JSON    │  já disponível em apiCalls.length│
│  → "dados incorretos"    │  → query extra por cliente       │
└──────────────────────────┴──────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    FOLLOW-UP (PR SEPARADO)                  │
├───────────────┬────────────────┬───────────────────────────┤
│  F1           │  F2            │  F3                        │
│  Bodies em    │  Sem paginação │  Sem try/catch             │
│  memória      │  (sem limit)   │  (500 não estruturado)     │
└───────────────┴────────────────┴───────────────────────────┘
```

---

## Estimativa de impacto

| Antes (1.000 clientes) | Depois |
|------------------------|--------|
| ~3.000 queries sequenciais | 3 queries paralelas |
| Timeout em staging (>30s) | < 500ms estimado |
| Dados incorretos (NaN) em clientes sem uso | Valores corretos (0) |
| IDOR — acesso cross-tenant | Bloqueado no handler |
