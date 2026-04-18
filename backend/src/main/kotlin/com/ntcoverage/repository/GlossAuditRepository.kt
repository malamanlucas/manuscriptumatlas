package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.GlossAuditExampleDTO
import com.ntcoverage.model.GlossAuditStatsDTO
import com.ntcoverage.model.GlossAuditVerdict
import com.ntcoverage.model.InterlinearGlossAudits
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class GlossAuditRepository {

    private val db get() = BibleDatabaseConfig.database

    fun insertVerdict(
        wordId: Int,
        glossSnapshot: String,
        verdict: String,
        suggestedPt: String?,
        reason: String?,
        judgeModel: String?
    ) = transaction(db) {
        InterlinearGlossAudits.insert {
            it[InterlinearGlossAudits.wordId] = wordId
            it[InterlinearGlossAudits.glossSnapshot] = glossSnapshot
            it[InterlinearGlossAudits.verdict] = verdict
            it[InterlinearGlossAudits.suggestedPt] = suggestedPt
            it[InterlinearGlossAudits.reason] = reason
            it[InterlinearGlossAudits.judgeModel] = judgeModel
            it[InterlinearGlossAudits.judgedAt] = OffsetDateTime.now()
        }
    }

    fun getStats(book: String?, chapter: Int?): GlossAuditStatsDTO = transaction(db) {
        val scopeClause = buildScopeClause(book, chapter)
        val statsSql = """
            WITH latest AS (
                SELECT DISTINCT ON (a.word_id)
                    a.word_id, a.verdict, a.resolved_at
                FROM interlinear_gloss_audits a
                JOIN interlinear_words iw ON iw.id = a.word_id
                JOIN bible_verses bv ON iw.verse_id = bv.id
                JOIN bible_books bb ON bv.book_id = bb.id
                $scopeClause
                ORDER BY a.word_id, a.judged_at DESC
            ),
            glosses AS (
                SELECT iw.id AS word_id
                FROM interlinear_words iw
                JOIN bible_verses bv ON iw.verse_id = bv.id
                JOIN bible_books bb ON bv.book_id = bb.id
                WHERE iw.portuguese_gloss IS NOT NULL
                  AND length(iw.portuguese_gloss) > 0
                  ${if (book != null) "AND bb.name = '${book.sqlEscape()}'" else ""}
                  ${if (chapter != null) "AND bv.chapter = $chapter" else ""}
            )
            SELECT
                (SELECT COUNT(*) FROM glosses) AS total,
                COUNT(*) FILTER (WHERE l.verdict = 'ok' AND l.resolved_at IS NULL) AS ok,
                COUNT(*) FILTER (WHERE l.verdict = 'bad_en' AND l.resolved_at IS NULL) AS bad_en,
                COUNT(*) FILTER (WHERE l.verdict = 'bad_es' AND l.resolved_at IS NULL) AS bad_es,
                COUNT(*) FILTER (WHERE l.verdict = 'bad_other' AND l.resolved_at IS NULL) AS bad_other,
                COUNT(*) FILTER (WHERE l.verdict = 'unknown' AND l.resolved_at IS NULL) AS unknown_c,
                (SELECT COUNT(*) FROM glosses g WHERE g.word_id NOT IN (SELECT word_id FROM latest)) AS pending
            FROM latest l;
        """.trimIndent()

        var total = 0
        var ok = 0
        var badEn = 0
        var badEs = 0
        var badOther = 0
        var unknown = 0
        var pending = 0

        exec(statsSql, explicitStatementType = StatementType.SELECT) { rs ->
            if (rs.next()) {
                total = rs.getInt("total")
                ok = rs.getInt("ok")
                badEn = rs.getInt("bad_en")
                badEs = rs.getInt("bad_es")
                badOther = rs.getInt("bad_other")
                unknown = rs.getInt("unknown_c")
                pending = rs.getInt("pending")
            }
        }

        val examples = fetchExamples(book, chapter, limit = 20)
        GlossAuditStatsDTO(
            total = total,
            ok = ok,
            badEn = badEn,
            badEs = badEs,
            badOther = badOther,
            unknown = unknown,
            pending = pending,
            examples = examples
        )
    }

    private fun fetchExamples(book: String?, chapter: Int?, limit: Int): List<GlossAuditExampleDTO> = transaction(db) {
        val conditions = mutableListOf("a.verdict LIKE 'bad%'", "a.resolved_at IS NULL")
        if (book != null) conditions.add("bb.name = '${book.sqlEscape()}'")
        if (chapter != null) conditions.add("bv.chapter = $chapter")
        val whereClause = "WHERE ${conditions.joinToString(" AND ")}"
        val sql = """
            SELECT DISTINCT ON (a.word_id)
                a.word_id, a.verdict, a.suggested_pt, a.reason,
                iw.portuguese_gloss, iw.original_word, iw.transliteration, iw.english_gloss, iw.word_position,
                bb.name AS book_name, bv.chapter AS chap, bv.verse_number AS verse
            FROM interlinear_gloss_audits a
            JOIN interlinear_words iw ON iw.id = a.word_id
            JOIN bible_verses bv ON iw.verse_id = bv.id
            JOIN bible_books bb ON bv.book_id = bb.id
            $whereClause
            ORDER BY a.word_id, a.judged_at DESC
            LIMIT $limit;
        """.trimIndent()

        val out = mutableListOf<GlossAuditExampleDTO>()
        exec(sql) { rs ->
            while (rs.next()) {
                out.add(
                    GlossAuditExampleDTO(
                        wordId = rs.getInt("word_id"),
                        book = rs.getString("book_name"),
                        chapter = rs.getInt("chap"),
                        verse = rs.getInt("verse"),
                        wordPosition = rs.getInt("word_position"),
                        originalWord = rs.getString("original_word"),
                        transliteration = rs.getString("transliteration"),
                        englishGloss = rs.getString("english_gloss"),
                        currentGloss = rs.getString("portuguese_gloss") ?: "",
                        verdict = rs.getString("verdict"),
                        suggestedPt = rs.getString("suggested_pt"),
                        reason = rs.getString("reason")
                    )
                )
            }
        }
        out
    }

    /** Seta portuguese_gloss=NULL para glosses com último verdict BAD (não resolvido) e marca audits como resolved. Retorna contagem de glosses nulificados. */
    fun nullifyFlaggedAndResolve(book: String?, chapter: Int?): Int = transaction(db) {
        val scopeFilter = buildScopeClause(book, chapter)
        val latestCte = """
            WITH latest AS (
                SELECT DISTINCT ON (a.word_id)
                    a.id AS audit_id, a.word_id, a.verdict, a.resolved_at
                FROM interlinear_gloss_audits a
                JOIN interlinear_words iw ON iw.id = a.word_id
                JOIN bible_verses bv ON iw.verse_id = bv.id
                JOIN bible_books bb ON bv.book_id = bb.id
                $scopeFilter
                ORDER BY a.word_id, a.judged_at DESC
            )
        """.trimIndent()

        val nullifyWords = """
            $latestCte
            UPDATE interlinear_words
            SET portuguese_gloss = NULL
            WHERE id IN (
                SELECT word_id FROM latest
                WHERE verdict LIKE 'bad%' AND resolved_at IS NULL
            );
        """.trimIndent()

        val markResolved = """
            $latestCte
            UPDATE interlinear_gloss_audits
            SET resolved_at = NOW()
            WHERE id IN (
                SELECT audit_id FROM latest
                WHERE verdict LIKE 'bad%' AND resolved_at IS NULL
            );
        """.trimIndent()

        val countSql = """
            $latestCte
            SELECT COUNT(*) AS c FROM latest
            WHERE verdict LIKE 'bad%' AND resolved_at IS NULL;
        """.trimIndent()

        var count = 0
        exec(countSql, explicitStatementType = StatementType.SELECT) { rs ->
            if (rs.next()) count = rs.getInt("c")
        }
        if (count > 0) {
            exec(nullifyWords, explicitStatementType = StatementType.UPDATE)
            exec(markResolved, explicitStatementType = StatementType.UPDATE)
        }
        count
    }

    private fun buildScopeClause(book: String?, chapter: Int?): String {
        val parts = mutableListOf<String>()
        if (book != null) parts.add("bb.name = '${book.sqlEscape()}'")
        if (chapter != null) parts.add("bv.chapter = $chapter")
        return if (parts.isEmpty()) "" else "WHERE ${parts.joinToString(" AND ")}"
    }

    private fun String.sqlEscape(): String = this.replace("'", "''")
}
