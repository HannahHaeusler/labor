/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
import com.acme.labor.entity.DokId
import com.acme.labor.service.LaborService
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * Den Microservice _dok_ mit WebClient aufrufen.
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface LogWebClientDok {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen, damit der Microservice _dok_
     * mit WebClient aufgerufen wird.
     * @param laborService LaborService mit WebClient Builder
     * @return CommandLineRunner
     */
    @Bean
    @Profile(DEV)
    @Suppress("LongMethod")
    fun logWebClientDok(laborService: LaborService) = CommandLineRunner {
        val logger: Logger = LogManager.getLogger(DbPopulate::class.java)

        runBlocking {
            val dokId = DokId.fromString("00000000-0000-0000-0000-000000000001")
            val dok = laborService.findDokById(dokId)
            logger.warn("Dok zur ID {}: {}", dokId, dok)
        }
    }

    // Fuer OAuth siehe
    // https://github.com/bclozel/spring-reactive-university/blob/master/src/main/java/com/example/integration/...
    //      ...gitter/GitterClient.java
}
