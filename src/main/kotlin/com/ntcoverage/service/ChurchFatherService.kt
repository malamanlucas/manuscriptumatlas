package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.ChurchFatherRepository
import com.ntcoverage.repository.FatherTextualStatementRepository

class ChurchFatherService(
    private val repository: ChurchFatherRepository,
    private val statementRepository: FatherTextualStatementRepository
) {
    fun listFathers(
        century: Int? = null,
        tradition: String? = null,
        page: Int = 1,
        limit: Int = 50,
        locale: String = "en"
    ): ChurchFathersListResponse {
        val fathers = repository.findAll(century, tradition, page, limit, locale)
        val total = repository.countAll(century, tradition)
        return ChurchFathersListResponse(total = total, fathers = fathers)
    }

    fun getFatherDetail(id: Int, locale: String = "en"): ChurchFatherDetail? =
        repository.findById(id, locale)

    fun searchFathers(query: String, limit: Int = 20, locale: String = "en"): List<ChurchFatherSummary> =
        repository.search(query, limit, locale)

    fun listStatements(
        topic: String? = null,
        century: Int? = null,
        tradition: String? = null,
        page: Int = 1,
        limit: Int = 20,
        locale: String = "en"
    ): TextualStatementsListResponse {
        val statements = statementRepository.findAll(topic, century, tradition, page, limit, locale)
        val total = statementRepository.countAll(topic, century, tradition)
        return TextualStatementsListResponse(total = total, statements = statements)
    }

    fun getStatementsByFather(fatherId: Int, locale: String = "en"): List<TextualStatementDTO> =
        statementRepository.findByFather(fatherId, locale)

    fun searchStatements(q: String, limit: Int = 20, locale: String = "en"): List<TextualStatementDTO> =
        statementRepository.searchByKeyword(q, limit, locale)

    fun getTopicsSummary(): TopicsSummaryResponse =
        TopicsSummaryResponse(topics = statementRepository.countByTopic())
}
