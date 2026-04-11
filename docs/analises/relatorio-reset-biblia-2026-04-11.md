# Relatorio de Reset — Aba Biblia (Todas as Camadas)

**Data:** 2026-04-11 ~18:37 UTC
**Acao:** Reset completo de todas as 4 camadas de ingestao biblica via UI

---

## Resultado: LIMPEZA COMPLETA CONFIRMADA

Todas as 12 tabelas do banco `bible_db` estao com **0 registros**.

### Contagem por Tabela

| Tabela | Registros | Status |
|--------|-----------|--------|
| `bible_versions` | 0 | Limpa |
| `bible_books` | 0 | Limpa |
| `bible_chapters` | 0 | Limpa |
| `bible_verses` | 0 | Limpa |
| `bible_verse_texts` | 0 | Limpa |
| `bible_book_abbreviations` | 0 | Limpa |
| `interlinear_words` | 0 | Limpa |
| `word_alignments` | 0 | Limpa |
| `greek_lexicon` | 0 | Limpa |
| `greek_lexicon_translations` | 0 | Limpa |
| `hebrew_lexicon` | 0 | Limpa |
| `hebrew_lexicon_translations` | 0 | Limpa |

### Fases de Ingestao

| Verificacao | Resultado |
|-------------|-----------|
| Fases `bible_*` em `council_ingestion_phases` | 0 registros |
| Items `bible_*` na `llm_prompt_queue` | 0 registros |
| Fila LLM total (outros dominios) | 405 items (nao-biblia) |

### Espaco em Disco (indices orfaos)

As tabelas estao vazias mas os **indices** ainda ocupam espaco alocado:

| Tabela | Espaco Total (dados + indices) | Dados |
|--------|-------------------------------|-------|
| `interlinear_words` | 3.624 KB | 0 bytes |
| `word_alignments` | 2.504 KB | 0 bytes |
| `hebrew_lexicon_translations` | 2.432 KB | 0 bytes |
| `greek_lexicon_translations` | 2.048 KB | 0 bytes |
| `hebrew_lexicon` | 1.400 KB | 0 bytes |
| `greek_lexicon` | 1.304 KB | 0 bytes |
| `bible_verse_texts` | 688 KB | 0 bytes |
| **Total** | **~14 MB** | **0 bytes** |

> Os indices serao reutilizados na proxima ingestao. Para liberar espaco agora: `VACUUM FULL` nas tabelas (desnecessario — sera preenchido em breve).

### Dead Tuples (autovacuum)

| Tabela | Dead Tuples | Autovacuum |
|--------|-------------|------------|
| `bible_versions` | 4 | pendente |
| `bible_books` | 1 | pendente |
| `bible_chapters` | 21 | pendente |
| Demais tabelas | 0 | concluido |

> Dead tuples sao residuos dos DELETEs do reset. O autovacuum ja processou as tabelas maiores. As menores (versions, books, chapters) serao limpas automaticamente pelo proximo ciclo.

---

## Conclusao

A limpeza esta **100% completa**. O banco `bible_db` esta pronto para uma nova ingestao completa das 4 camadas, agora sem filtro nas Camadas 1-3 (todos os 66 livros).
