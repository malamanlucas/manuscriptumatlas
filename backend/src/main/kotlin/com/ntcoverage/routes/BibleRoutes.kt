package com.ntcoverage.routes

import com.ntcoverage.service.BibleService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bibleRoutes(bibleService: BibleService) {

    // All literal routes use full paths (no route block to avoid Ktor dynamic route conflicts)

    get("/bible/versions") {
        val testament = call.request.queryParameters["testament"]
        call.respond(bibleService.getVersions(testament))
    }

    get("/bible/books") {
        val testament = call.request.queryParameters["testament"]
        val locale = call.request.queryParameters["locale"] ?: "en"
        call.respond(bibleService.getBooks(testament, locale))
    }

    get("/bible/search") {
        val query = call.request.queryParameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing query parameter 'q'"))
        val version = call.request.queryParameters["version"]
        val testament = call.request.queryParameters["testament"]
        val book = call.request.queryParameters["book"]
        val locale = call.request.queryParameters["locale"] ?: "en"
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
        call.respond(bibleService.searchText(query, version, testament, book, locale, page, limit))
    }

    get("/bible/compare/{book}/{chapter}/{verse}") {
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        val verse = call.parameters["verse"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid verse"))
        val versions = call.request.queryParameters["versions"]?.split(",")?.map { it.trim() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing versions parameter"))
        call.respond(bibleService.compareVerse(book, chapter, verse, versions))
    }

    get("/bible/compare/{book}/{chapter}") {
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        val versions = call.request.queryParameters["versions"]?.split(",")?.map { it.trim() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing versions parameter"))
        call.respond(bibleService.compareChapter(book, chapter, versions))
    }

    get("/bible/interlinear/{book}/{chapter}/{verse}") {
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        val verse = call.parameters["verse"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid verse"))
        val alignVersion = call.request.queryParameters["alignVersion"]
        call.respond(bibleService.getInterlinearVerse(book, chapter, verse, alignVersion))
    }

    get("/bible/interlinear/{book}/{chapter}") {
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        val alignVersion = call.request.queryParameters["alignVersion"]
        call.respond(bibleService.getInterlinearChapter(book, chapter, alignVersion))
    }

    get("/bible/strongs/{strongsNumber}") {
        val strongsNumber = call.parameters["strongsNumber"]!!
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 200)
        call.respond(bibleService.getStrongsConcordance(strongsNumber, page, limit))
    }

    get("/bible/lexicon/{strongsNumber}") {
        val strongsNumber = call.parameters["strongsNumber"]!!
        val locale = call.request.queryParameters["locale"] ?: "en"
        call.respond(bibleService.getLexiconEntry(strongsNumber, locale))
    }

    get("/bible/ref/{reference...}") {
        val reference = call.parameters.getAll("reference")?.joinToString("/")
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing reference"))
        val locale = call.request.queryParameters["locale"] ?: "en"
        call.respond(bibleService.resolveReference(java.net.URLDecoder.decode(reference, "UTF-8"), locale))
    }

    // Dynamic reader routes — use /bible/read/ prefix to avoid conflicts with literal routes
    get("/bible/read/{version}/{book}/{chapter}/{verse}") {
        val version = call.parameters["version"]!!
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        val verse = call.parameters["verse"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid verse"))
        call.respond(bibleService.getVerse(version, book, chapter, verse))
    }

    get("/bible/read/{version}/{book}/{chapter}") {
        val version = call.parameters["version"]!!
        val book = call.parameters["book"]!!
        val chapter = call.parameters["chapter"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid chapter"))
        call.respond(bibleService.getChapter(version, book, chapter))
    }
}
