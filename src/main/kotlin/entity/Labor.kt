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
package com.acme.labor.entity

import com.acme.labor.entity.Labor.Companion.ID_PATTERN
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * Unveränderliche Daten einer Labor. In DDD ist Labor ist ein _Aggregate Root_.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @property id ID einer Labor als UUID [ID_PATTERN]].
 * @property version Versionsnummber in der DB.
 * @property datum Bestelldatum.
 * @property dokId ID des zugehörigen Doks.
 * @property bestellpositionen Liste von [Bestellposition]
 * @property dokNachname Nachname des Doks. Der Nachname wird nicht in der DB gespeichert.
 */
@JsonPropertyOrder("datum", "dokId", "dokNachname", "bestellpositionen")
@Suppress("UnusedPrivateMember")
data class Labor(
    @JsonIgnore
    val id: LaborId? = null,

    @Version
    @JsonIgnore
    val version: Int? = null,

    val datum: LocalDate = now(),

    val dokId: DokId,

    @get:NotEmpty(message = "{labor.bestellpositionen.notEmpty}")
    @get:Valid
    val bestellpositionen: List<Bestellposition> = emptyList(),

    @CreatedDate
    @JsonIgnore
    private val erzeugt: LocalDateTime? = null,

    @LastModifiedDate
    @JsonIgnore
    private val aktualisiert: LocalDateTime? = null,
) {
    @Transient
    @Suppress("DataClassShouldBeImmutable", "UndocumentedPublicProperty")
    var dokNachname: String? = null

    /**
     * Vergleich mit einem anderen Objekt oder null.
     * @param other Das zu vergleichende Objekt oder null
     * @return True, falls das zu vergleichende (Dok-) Objekt die gleiche
     *      ID und die gleiche Dok-ID hat.
     */
    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Labor
        return id == other.id && dokId == other.dokId
    }

    /**
     * Hashwert aufgrund der Emailadresse.
     * @return Der Hashwert.
     */
    override fun hashCode(): Int {
        val result = id?.hashCode() ?: 0
        @Suppress("MagicNumber")
        return 31 * result + dokId.hashCode()
    }

    companion object {
        private const val HEX_PATTERN = "[\\dA-Fa-f]"

        /**
         * Muster bzw. regulärer Ausdruck für eine UUID.
         */
        const val ID_PATTERN = "$HEX_PATTERN{8}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{12}"
    }
}

typealias LaborId = UUID
typealias DokId = UUID
