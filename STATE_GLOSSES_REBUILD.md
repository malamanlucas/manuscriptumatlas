# Checkpoint — Reset glosses PT (bible_translate_glosses)

**Salvo em:** 2026-05-01 18:10 BRT (sessão pausada para restart da máquina).

## Resumo do progresso

Plano completo: `~/.claude/plans/image-1-image-2-whimsical-eich.md` (5 fases).

| Fase | Status |
|------|--------|
| 1 — Hardening de código | ✅ completo (com 2 hotfixes para falsos positivos no detector) |
| 2 — Backup + wipe portuguese_gloss NT | ✅ completo |
| 3.1 — Enfileirar bible_translate_glosses PT | ✅ completo (4853 itens enfileirados) |
| 3.2 — **Drenar fila** | 🟡 EM ANDAMENTO — 2.372 applied, 3.377 pending |
| 3.3 — Apply | ⏳ implícito após drain |
| 3.4 — QA intermediário | ⏳ pendente |
| 4 — Auditoria LLM-as-judge | ⏳ pendente |
| 5 — QA final + Playwright | ⏳ pendente |

## Estado snapshot (banco e fila)

```
queue:  pending=3377, processing=100 (stuck após kill do agent), completed=0, applied=2372, failed=0
db:     pt_populated=11749, pt_null=129773, total=141522, pct=8.30%
backup: tabela _backup_iw_pt_gloss_2026_05_01 com 141522 linhas (rollback se necessário)
```

## Mudanças de código aplicadas (já commitadas no fat jar atual)

### `IngestionScope` (BibleIngestionService.kt:50)
Tornou `bookName` nullable, adicionou campo `locales: List<String>?`.

### `translateGlossesPrepare` (BibleIngestionService.kt:1684)
`for (locale in scope?.locales ?: listOf("pt", "es"))` — respeita filtro de locale.

### `RunPhaseScopedRequest` (BibleLayer4Applications.kt) + rota `/run/{phase}` (AdminRoutes.kt:321)
Aceitam `{"locales":["pt"]}` no body.

### `LlmResponseProcessor.kt`
- `applyGlossTranslation` — hard-fail anti-cópia (`echo_english`) via `validateGloss`.
- `NON_PT_TOKENS` expandido (~120 palavras EN+ES distintas).
- `NON_ES_TOKENS` simétrico (reservado).
- `getEnglishGloss(wordId)` adicionado ao `InterlinearRepository`.
- **HOTFIX-1**: removido `looksLikeBareEnglish` (rejeitava PT ASCII válido como "suponho", "mulher", "entrei", "azeite").
- **HOTFIX-2**: removido `"do"` da NON_PT_TOKENS (rejeitava contração PT "do livro", "do que").
- Removido também: `"para", "por", "con", "sin", "si", "y", "está", "santo", "este", "contra", "todo", "todos", "como", "porque", "antes", "nunca"` — todas existem em PT.
- Mantido: `"y"` em NON_PT (só ES, claramente).

### System prompt do Sonnet (BibleIngestionService.kt:1691)
Blocklist densa, regra anti-cópia explícita, 14 few-shots PT cobrindo casos problemáticos identificados.

## Comandos de retomada (após restart)

```bash
# 1. Levantar Docker
cd ~/Documents/GitHub/manuscriptumatlas
docker-compose -f deploy/docker-compose.yml up -d

# 2. Aguardar app health
until curl -sf http://localhost:8080/health; do sleep 2; done

# 3. Token + unstick stale (libera os 100 stuck em "processing" do kill)
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "$TOKEN" > /tmp/atlas_token.txt
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick?staleMinutes=0"

# 4. Conferir estado da fila
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats \
  | python3 -m json.tool | grep -A 6 bible_translate_glosses

# 5. Conferir estado do banco
docker exec -e PGPASSWORD=postgres deploy-postgres-1 psql -U postgres -d bible_db -c "
SELECT COUNT(*) FILTER (WHERE iw.portuguese_gloss IS NOT NULL) AS pt_ok,
       COUNT(*) FILTER (WHERE iw.portuguese_gloss IS NULL) AS pt_null,
       COUNT(*) AS total
FROM interlinear_words iw
JOIN bible_verses bv ON bv.id = iw.verse_id
JOIN bible_books bb ON bb.id = bv.book_id
WHERE bb.testament = 'NT' AND iw.language = 'greek';"

# 6. Relançar dispatcher (na sessão Claude Code, invocar Agent com queue-dispatcher)
#    OU pedir ao Claude: "continue da onde parou — ler STATE_GLOSSES_REBUILD.md"
```

## Próximos passos (em ordem)

1. **Continuar Fase 3.2** — relançar `queue-dispatcher` para drenar os ~3477 pending restantes.
   - Esperar `pending+processing+completed = 0` ou novo falso positivo aparecer nos logs.
   - Monitorar logs do app: `docker logs deploy-app-1 --since 5m | grep "rejected gloss"` para detectar palavras PT rejeitadas indevidamente.

2. **Re-rodar Fase 3.1** — após primeira drenagem, há ~129K tokens com `portuguese_gloss=NULL` (ou os ~118K que sobrarem após a fila atual). Disparar nova rodada `bible_translate_glosses` com `{"locales":["pt"]}` — vai re-enfileirar todos os NULL para nova tentativa Sonnet.

3. **Ciclar Fase 3.2 + 3.1** até `pct_populated >= 95%`.

4. **Fase 3.4** — QA intermediário (queries no plan).

5. **Fase 4** — auditoria `bible_audit_glosses_pt`.

6. **Fase 5** — QA final + Playwright.

## Riscos

- Se `pending+processing+completed` ainda for >100 ao retomar, primeiro rodar `unstick?staleMinutes=0` para liberar os items stuck.
- Detector pode ainda ter falsos positivos não detectados — sempre monitorar logs nos primeiros 5min após relançar dispatcher.
- Se o LLM Sonnet continuar emitindo inglês mesmo com prompt reforçado, considerar upgrade temporário para HIGH (Opus) editando `tier = "MEDIUM"` → `"HIGH"` em BibleIngestionService.kt:1734.

## Como pedir ao Claude para retomar

> "Continue da fase 3.2 da reconstrução dos glosses PT. Ler `STATE_GLOSSES_REBUILD.md` para contexto. Plano completo em `~/.claude/plans/image-1-image-2-whimsical-eich.md`."
