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
package com.acme.labor.config

import com.acme.labor.Router
import com.acme.labor.config.db.CustomConversions
import com.acme.labor.config.db.GenerateLaborId
import com.acme.labor.config.db.TransactionManager
import com.acme.labor.config.security.AuthorizationConfig
import com.acme.labor.config.security.CustomUserDetailService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.hateoas.config.EnableHypermediaSupport
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL
import org.springframework.hateoas.support.WebStack.WEBFLUX
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

/**
 * Konfigurationsklasse für die Anwendung bzw. den Microservice.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Configuration(proxyBeanMethods = false)
@EnableHypermediaSupport(type = [HAL], stacks = [WEBFLUX])
@EnableWebFluxSecurity
@EnableMongoAuditing
@EnableConfigurationProperties(MailProps::class)
class AppConfig :
    Router,
    GenerateLaborId,
    CustomConversions,
    TransactionManager,
    AuthorizationConfig,
    CustomUserDetailService,
    WebClientBuilderConfig
