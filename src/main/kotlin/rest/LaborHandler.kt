/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.acme.labor.rest

import com.acme.labor.Router.Companion.idPathVar
import com.acme.labor.entity.DokId
import com.acme.labor.entity.Labor
import com.acme.labor.service.CreateResult
import com.acme.labor.service.LaborService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.hateoas.server.reactive.toCollectionModelAndAwait
import org.springframework.hateoas.server.reactive.toModelAndAwait
import org.springframework.http.HttpHeaders.IF_NONE_MATCH
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI
import javax.validation.ConstraintViolation

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.labor.Router.router] aufgerufen,
 * nimmt einen Request entgegen und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen LaborHandler mit einem injizierten [LaborService] erzeugen.
 */
@Component
class LaborHandler(private val service: LaborService, private val modelAssembler: LaborModelAssembler) {
    /**
     * Suche anhand der Labor-ID
     * @param request Der eingehende Request
     * @return Ein Response mit dem Statuscode 200 und der gefundenen Labor einschließlich Atom-Links,
     *      oder aber Statuscode 204.
     */
    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)

        val labor = service.findById(id) ?: return notFound().buildAndAwait()
        logger.debug("findById: {}", labor)

        val version = labor.version
        val versionHeader = request.headers()
            .header(IF_NONE_MATCH)
            .firstOrNull()
            ?.toIntOrNull()

        if (version == versionHeader) {
            return status(NOT_MODIFIED).buildAndAwait()
        }

        val laborModel = modelAssembler.toModelAndAwait(labor, request.exchange())
        // Entity Tag, um Aenderungen an der angeforderten Ressource erkennen zu koennen.
        // Client: GET-Requests mit Header "If-None-Match"
        //         ggf. Response mit Statuscode NOT MODIFIED (s.o.)
        return ok().eTag("\"$version\"").bodyValueAndAwait(laborModel)
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird eine Liste zurückgeliefert, damit auch der
     * Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Response mit dem Statuscode 200 und einer Liste mit den gefundenen Labore einschließlich
     *      Atom-Links, oder aber Statuscode 204.
     */
    @Suppress("ReturnCount", "LongMethod")
    suspend fun find(request: ServerRequest): ServerResponse {
        val queryParams = request.queryParams()
        if (queryParams.size > 1) {
            return notFound().buildAndAwait()
        }

        val labore = if (queryParams.isEmpty()) {
            service.findAll()
        } else {
            val dokId = request.queryParam("dokId")
            if (!dokId.isPresent) {
                return notFound().buildAndAwait()
            }

            service.findByDokId(DokId.fromString(dokId.get()))
        }

        val laboreList = mutableListOf<Labor>()
        labore.toList(laboreList)

        return if (laboreList.isEmpty()) {
            notFound().buildAndAwait()
        } else {
            val laboreModel =
                modelAssembler.toCollectionModelAndAwait(laboreList.asFlow(), request.exchange())
            logger.debug("find: {}", laboreModel)
            ok().bodyValueAndAwait(laboreModel)
        }
    }

    /**
     * Einen neuen Labor-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Labor-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    suspend fun create(request: ServerRequest): ServerResponse {
        val labor = request.awaitBody<Labor>()

        return when (val result = service.create(labor)) {
            is CreateResult.Success -> handleCreated(result.labor, request)
            is CreateResult.ConstraintViolations -> handleConstraintViolations(result.violations)
        }
    }

    private suspend fun handleCreated(labor: Labor, request: ServerRequest): ServerResponse {
        logger.debug("handleCreated: {}", labor)
        val baseUri = getBaseUri(request.headers().asHttpHeaders(), request.uri())
        val location = URI("$baseUri/${labor.id}")
        logger.debug("handleCreated: {}", location)
        return created(location).buildAndAwait()
    }

    // z.B. Service-Funktion "create|update" mit Parameter "labor" hat dann
    // Meldungen mit "create.labor.nachname:"
    private suspend fun handleConstraintViolations(violations: Set<ConstraintViolation<Labor>>): ServerResponse {
        if (violations.isEmpty()) {
            return ServerResponse.badRequest().buildAndAwait()
        }

        val laborViolations = violations.map { violation ->
            LaborConstraintViolation(property = violation.propertyPath.toString(), message = violation.message)
        }
        logger.trace("violations: {}", laborViolations)
        return ServerResponse.badRequest().bodyValueAndAwait(laborViolations)
    }

    private companion object {
        val logger: Logger = LogManager.getLogger(LaborHandler::class.java)
    }
}
