/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.service

import com.acme.labor.entity.Dok
import com.acme.labor.entity.DokId
import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import javax.validation.ConstraintViolation
import javax.validation.Validator

/**
 * Anwendungslogik für Labore.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborService(
    private val mongo: ReactiveFluentMongoOperations,
    @Lazy private val validator: Validator,
    // siehe org.springframework.web.reactive.function.client.DefaultWebClientBuilder
    // siehe org.springframework.web.reactive.function.client.DefaultWebClient
    @Lazy private val clientBuilder: WebClient.Builder,
) {
    /**
     * Alle Labore ermitteln.
     * @return Alle Labore.
     */
    suspend fun findAll(): Flow<Labor> = mongo.query<Labor>()
        .flow()
        .onEach { labor ->
            logger.debug("findAll: {}", labor)
            val dok = findDokById(labor.dokId)
            labor.dokNachname = dok.nachname
        }

    /**
     * Eine Labor anhand der ID suchen.
     * @param id Die Id der gesuchten Labor.
     * @return Die gefundene Labor oder null.
     */
    suspend fun findById(id: String): Labor? {
        val labor = mongo.query<Labor>()
            .matching(query(Labor::id isEqualTo id))
            .awaitOneOrNull()
        logger.debug("findById: {}", labor)
        if (labor == null) {
            return labor
        }

        val (nachname) = findDokById(labor.dokId)
        return labor.apply { dokNachname = nachname }
    }

    /**
     * Dok anhand der Dok-ID suchen.
     * @param dokId Die Id des gesuchten Doks.
     * @return Der gefundene Dok oder null.
     */
    suspend fun findDokById(dokId: DokId): Dok {
        logger.debug("findDokById: {}", dokId)

        // org.springframework.web.reactive.function.client.DefaultWebClient
        val client = clientBuilder
            .baseUrl("http://$dokService:$dokPort")
            .filter(basicAuthentication(username, password))
            .build()

        return client
            .get()
            .uri("/api/$dokId")
            .retrieve()
            .awaitBody()
    }

    /**
     * Labore zur Dok-ID suchen.
     * @param dokId Die Id des gegebenen Doks.
     * @return Die gefundenen Labore oder ein leeres Flux-Objekt.
     */
    suspend fun findByDokId(dokId: DokId): Flow<Labor> {
        val (nachname) = findDokById(dokId)

        val criteria = where(Labor::dokId).regex("\\.*$dokId\\.*", "i")
        return mongo.query<Labor>().matching(Query(criteria))
            .flow()
            .onEach { labor ->
                logger.debug("findByDokId: {}", labor)
                labor.dokNachname = nachname
            }
    }

    /**
     * Eine neue Labor anlegen.
     * @param labor Das Objekt der neu anzulegenden Labor.
     * @return Die neu angelegte Labor mit generierter ID.
     */
    suspend fun create(labor: Labor): CreateResult {
        logger.debug("create: {}", labor)
        val violations = validator.validate(labor)
        if (violations.isNotEmpty()) {
            return CreateResult.ConstraintViolations(violations)
        }

        val neueLabor = mongo.insert<Labor>().oneAndAwait(labor)
        return CreateResult.Success(neueLabor)
    }

    companion object {
        /**
         * Rechnername des Dok-Service durch _Service Registry_ von Kubernetes (und damit Istio).
         */
        // https://github.com/istio/istio/blob/master/samples/bookinfo/src/reviews/reviews-application/src/main/java/application/rest/LibertyRestEndpoint.java#L43
        val dokService = System.getenv("DOK_HOSTNAME") ?: "dok"

        /**
         * Port des Dok-Service durch _Service Registry_ von Kubernetes (und damit Istio).
         */
        val dokPort = System.getenv("DOK_SERVICE_PORT") ?: "8080"

        private const val username = "admin"
        private const val password = "p"
        private val logger: Logger = LogManager.getLogger(LaborService::class.java)
    }
}

/**
 * Resultat-Typ für [LaborService.create]
 */
sealed class CreateResult {
    /**
     * Resultat-Typ, wenn eine neue Labor erfolgreich angelegt wurde.
     * @property labor Die neu angelegte Labor
     */
    data class Success(val labor: Labor) : CreateResult()

    /**
     * Resultat-Typ, wenn eine Labor wegen Constraint-Verletzungen nicht angelegt wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Set<ConstraintViolation<Labor>>) : CreateResult()
}
