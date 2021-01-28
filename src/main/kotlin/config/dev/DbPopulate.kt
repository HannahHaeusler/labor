/*
 * Copyright (C) 2016 - 2018 Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.config.dev

import com.acme.labor.config.Settings.DEV
import com.acme.labor.config.dev.Daten.labore
import com.acme.labor.entity.Labor
import com.acme.labor.entity.Labor.Companion.ID_PATTERN
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Description
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.array
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.date
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string
import org.springframework.data.mongodb.core.schema.MongoJsonSchema

/**
 * Interface, um im Profil _dev_ die (Test-) DB neu zu laden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface DbPopulate {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen, damit die (Test-) DB neu
     * geladen wird.
     * @param mongo Template für MongoDB
     * @return CommandLineRunner
     */
    @Bean
    @Description("DB neu laden")
    @Profile(DEV)
    fun dbPopulate(mongo: ReactiveMongoOperations) = CommandLineRunner {
        val logger: Logger = LogManager.getLogger(DbPopulate::class.java)
        logger.warn("Neuladen der Collection 'Labor'")

        runBlocking {
            mongo.dropCollection<Labor>().awaitFirstOrNull()
            createSchema(mongo, logger)
            createIndex(mongo, logger)
            labore.onEach { labor -> mongo.insert<Labor>().oneAndAwait(labor) }
                .collect { labor -> logger.warn("{}", labor) }
        }
    }

    private suspend fun createSchema(mongoOps: ReactiveMongoOperations, logger: Logger): MongoCollection<Document> {
        // https://docs.mongodb.com/manual/core/schema-validation/
        // https://www.mongodb.com/blog/post/mongodb-36-json-schema-validation-expressive-query-syntax
        val schema = MongoJsonSchema.builder()
            .required("datum", "dokId", "bestellpositionen")
            .properties(
                date("datum"),
                string("dokId").matching(ID_PATTERN),
                array("bestellpositionen").uniqueItems(true),
            )
            .build()

        logger.info("JSON Schema fuer Labor: {}", { schema.toDocument().toJson() })
        return mongoOps.createCollection<Labor>(CollectionOptions.empty().schema(schema)).awaitFirst()
    }

    private suspend fun createIndex(mongoOps: ReactiveMongoOperations, logger: Logger): String {
        logger.warn("Index fuer 'dokId'")
        val idx = Index("dokId", Sort.Direction.ASC).named("dokId")
        return mongoOps.indexOps<Labor>().ensureIndex(idx).awaitFirst()
    }
}
