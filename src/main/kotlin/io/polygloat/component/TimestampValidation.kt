/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.component

import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.dtos.request.validators.exceptions.ValidationException
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Component
import java.util.*

@Component
class TimestampValidation(
        private val aes: Aes,
        private val polygloatProperties: PolygloatProperties
) {
    fun checkTimeStamp(
            timestamp: String,
            maxAgeInMs: Long = polygloatProperties.authentication.timestampMaxAge
    ) {
        if (!isTimeStampValid(timestamp, maxAgeInMs)) {
            throw ValidationException(Message.INVALID_TIMESTAMP)
        }
    }

    fun isTimeStampValid(
            encryptedTimestamp: String,
            maxAgeInMs: Long = polygloatProperties.authentication.timestampMaxAge
    ): Boolean {
        val timestampDecoded = decryptTimeStamp(encryptedTimestamp)
        val now = Date().time
        return now - maxAgeInMs <= timestampDecoded
    }

    fun encryptTimeStamp(timestamp: Long): String {
        val byteArray = timestamp.toString().toByteArray(charset("ASCII"))
        return Hex.encodeHexString(aes.encrypt(byteArray))
    }

    fun decryptTimeStamp(timestamp: String): Long {
        val byteArray = Hex.decodeHex(timestamp)
        val decrypted = aes.decrypt(byteArray)
        val timestampString = decrypted.toString(charset("ASCII"))
        return timestampString.toLong()
    }
}