package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.model.VerseManuscriptItem
import com.ntcoverage.model.VerseManuscriptsResponse
import com.ntcoverage.repository.VerseRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.verseRoutes(verseRepository: VerseRepository) {
    get("/verses/manuscripts") {
        val book = call.request.queryParameters["book"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Query parameter 'book' required"))
        val chapter = call.request.queryParameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid 'chapter' number required"))
        val verse = call.request.queryParameters["verse"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid 'verse' number required"))
        val type = call.request.queryParameters["type"]?.takeIf { it in listOf("papyrus", "uncial") }

        val bookId = verseRepository.findBookIdByName(book)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Book not found: $book"))
        val verseId = verseRepository.findVerseId(bookId, chapter, verse)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Verse not found: $book $chapter:$verse"))

        val list = verseRepository.getManuscriptsForVerse(verseId, type).map { m ->
            VerseManuscriptItem(
                gaId = m.gaId,
                name = m.name,
                centuryMin = m.centuryMin,
                centuryMax = m.centuryMax,
                type = m.type,
                ntvmrUrl = m.ntvmrUrl
            )
        }
        call.respond(VerseManuscriptsResponse(book = book, chapter = chapter, verse = verse, manuscripts = list))
    }
}
