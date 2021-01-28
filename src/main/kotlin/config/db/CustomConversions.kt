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
package com.acme.labor.config.db

import com.acme.labor.entity.LaborId
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

/**
 * Spring-Konfiguration für Enum-Konvertierungen beim Zugriff auf _MongoDB_.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface CustomConversions {
    /**
     * Liste mit Konvertern für Lesen und Schreiben in _MongoDB_ ermitteln.
     * @return Liste mit Konvertern für Lesen und Schreiben in _MongoDB_.
     */
    @Bean
    fun customConversions() = MongoCustomConversions(
        listOfNotNull(LaborIdConverter.ReadConverter(), LaborIdConverter.WriteConverter())
    )

    /**
     * Konverterklassen, um LaborIds in _MongoDB_ zu speichern und auszulesen.
     *
     * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
     */
    interface LaborIdConverter {
        /**
         * Konvertierungsklasse für MongoDB, um einen String einzulesen und eine LaborId zu erzeugen.
         * Wegen @ReadingConverter ist kein Lambda-Ausdruck möglich.
         */
        @ReadingConverter
        class ReadConverter : Converter<String, LaborId> {
            /**
             * Konvertierung eines Strings in eine LaborId.
             * @param laborId String mit einer LaborId.
             * @return Zugehörige LaborId
             */
            override fun convert(laborId: String): LaborId = LaborId.fromString(laborId)
        }

        /**
         * Konvertierungsklasse für MongoDB, um eine LaborId in einen String zu konvertieren.
         * Wegen @WritingConverter ist kein Lambda-Ausdruck möglich.
         */
        @WritingConverter
        class WriteConverter : Converter<LaborId, String> {
            /**
             * Konvertierung einer LaborId in einen String, z.B. beim Abspeichern.
             * @param laborId Objekt von LaborId
             * @return String z.B. zum Abspeichern.
             */
            override fun convert(laborId: LaborId): String? = laborId.toString()
        }
    }
}
