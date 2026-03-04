package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class CouncilService(
    private val councilRepository: CouncilRepository,
    private val sourceRepository: SourceRepository,
    private val claimRepository: CouncilSourceClaimRepository,
    private val heresyRepository: HeresyRepository,
    private val canonRepository: CouncilCanonRepository
) {
    fun listCouncils(
        century: Int? = null,
        type: String? = null,
        yearMin: Int? = null,
        yearMax: Int? = null,
        page: Int = 1,
        limit: Int = 20,
        locale: String = "en"
    ): CouncilsListResponse {
        val councils = councilRepository.findAll(century, type, yearMin, yearMax, page, limit, locale)
        val total = councilRepository.countAll(century, type, yearMin, yearMax)
        return CouncilsListResponse(total = total, councils = councils)
    }

    fun searchCouncils(query: String, limit: Int = 20, locale: String = "en"): List<CouncilSummaryDTO> =
        councilRepository.search(query, limit, locale)

    fun getCouncilTypeSummary(): List<CouncilTypeSummaryDTO> = councilRepository.listTypeSummary()

    fun getCouncilMapPoints(): List<CouncilMapPointDTO> = councilRepository.listMapPoints()

    fun getCouncilDetail(slug: String, locale: String = "en"): CouncilDetailDTO? {
        val detail = councilRepository.findBySlug(slug, locale) ?: return null
        val canons = canonRepository.countByCouncilId(detail.id)
        val fathers = getCouncilFathers(slug, locale)
        val heresies = getCouncilHeresies(slug, locale)
        val sources = getCouncilSources(slug)

        return detail.copy(
            relatedFathers = fathers,
            heresies = heresies,
            canonCount = canons,
            sourceClaims = sources
        )
    }

    fun getCouncilFathers(slug: String, locale: String = "en"): List<CouncilFatherDTO> = transaction {
        val councilId = resolveCouncilId(slug) ?: return@transaction emptyList()
        val join = CouncilFathers
            .join(ChurchFathers, JoinType.INNER, additionalConstraint = { CouncilFathers.fatherId eq ChurchFathers.id })
            .join(ChurchFatherTranslations, JoinType.LEFT, additionalConstraint = {
                (ChurchFathers.id eq ChurchFatherTranslations.fatherId) and (ChurchFatherTranslations.locale eq locale)
            })

        join.selectAll()
            .where { CouncilFathers.councilId eq councilId }
            .map {
                CouncilFatherDTO(
                    fatherId = it[ChurchFathers.id].value,
                    fatherName = if (locale == "en") it[ChurchFathers.displayName]
                    else it.getOrNull(ChurchFatherTranslations.displayName) ?: it[ChurchFathers.displayName],
                    role = it[CouncilFathers.role]
                )
            }
    }

    fun getCouncilCanons(slug: String, page: Int = 1, limit: Int = 50): List<CouncilCanonDTO> =
        canonRepository.findByCouncilSlug(slug, page, limit)

    fun getCouncilHeresies(slug: String, locale: String = "en"): List<HeresySummaryDTO> = transaction {
        val councilId = resolveCouncilId(slug) ?: return@transaction emptyList()
        val join = CouncilHeresies
            .join(Heresies, JoinType.INNER, additionalConstraint = { CouncilHeresies.heresyId eq Heresies.id })
            .join(HeresyTranslations, JoinType.LEFT, additionalConstraint = {
                (Heresies.id eq HeresyTranslations.heresyId) and (HeresyTranslations.locale eq locale)
            })
        join.selectAll()
            .where { CouncilHeresies.councilId eq councilId }
            .map {
                HeresySummaryDTO(
                    id = it[Heresies.id].value,
                    name = if (locale == "en") it[Heresies.name]
                    else it.getOrNull(HeresyTranslations.name) ?: it[Heresies.name],
                    slug = it[Heresies.slug],
                    centuryOrigin = it[Heresies.centuryOrigin],
                    yearOrigin = it[Heresies.yearOrigin],
                    keyFigure = it[Heresies.keyFigure]
                )
            }
    }

    fun getCouncilSources(slug: String): List<SourceClaimDTO> {
        val councilId = transaction { resolveCouncilId(slug) } ?: return emptyList()
        return claimRepository.findByCouncil(councilId).map {
            SourceClaimDTO(
                sourceDisplayName = it.sourceDisplayName,
                sourceLevel = it.sourceLevel,
                claimedYear = it.claimedYear,
                claimedYearEnd = it.claimedYearEnd,
                claimedLocation = it.claimedLocation,
                claimedParticipants = it.claimedParticipants,
                sourcePage = it.sourcePage,
                rawText = it.rawText
            )
        }
    }

    fun listHeresies(page: Int = 1, limit: Int = 20, locale: String = "en"): HeresiesListResponse =
        HeresiesListResponse(
            total = heresyRepository.countAll(),
            heresies = heresyRepository.findAll(page, limit, locale)
        )

    fun getHeresyDetail(slug: String, locale: String = "en"): HeresyDetailDTO? =
        heresyRepository.findBySlug(slug, locale)

    fun getHeresyCouncils(slug: String, locale: String = "en"): List<CouncilSummaryDTO> =
        heresyRepository.listCouncilsByHeresySlug(slug, locale)

    fun listSources(): List<SourceDTO> = sourceRepository.findAll()

    fun getCouncilsByFather(fatherId: Int, locale: String = "en"): List<CouncilSummaryDTO> = transaction {
        val join = CouncilFathers
            .join(Councils, JoinType.INNER, additionalConstraint = { CouncilFathers.councilId eq Councils.id })
            .join(CouncilTranslations, JoinType.LEFT, additionalConstraint = {
                (Councils.id eq CouncilTranslations.councilId) and (CouncilTranslations.locale eq locale)
            })
        join.selectAll()
            .where { CouncilFathers.fatherId eq fatherId }
            .map {
                CouncilSummaryDTO(
                    id = it[Councils.id].value,
                    displayName = if (locale == "en") it[Councils.displayName]
                    else it.getOrNull(CouncilTranslations.displayName) ?: it[Councils.displayName],
                    slug = it[Councils.slug],
                    year = it[Councils.year],
                    yearEnd = it[Councils.yearEnd],
                    century = it[Councils.century],
                    councilType = it[Councils.councilType],
                    location = if (locale == "en") it[Councils.location]
                    else it.getOrNull(CouncilTranslations.location) ?: it[Councils.location],
                    latitude = it[Councils.latitude],
                    longitude = it[Councils.longitude],
                    numberOfParticipants = it[Councils.numberOfParticipants],
                    consensusConfidence = it[Councils.consensusConfidence],
                    dataConfidence = it[Councils.dataConfidence],
                    sourceCount = it[Councils.sourceCount]
                )
            }
    }

    private fun resolveCouncilId(slug: String): Int? =
        Councils.select(Councils.id)
            .where { Councils.slug eq slug }
            .singleOrNull()
            ?.get(Councils.id)
            ?.value
}
