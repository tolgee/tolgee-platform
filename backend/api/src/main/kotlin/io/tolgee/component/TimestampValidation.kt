/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Component
import java.util.*

@Component
class TimestampValidation(
  private val aes: Aes,
  private val tolgeeProperties: TolgeeProperties
) {
  fun checkTimeStamp(
    entityUnique: String,
    encryptedTimestamp: String?,
    maxAgeInMs: Long = tolgeeProperties.authentication.securedImageTimestampMaxAge
  ) {
    if (!isTimeStampValid(entityUnique, encryptedTimestamp, maxAgeInMs)) {
      throw ValidationException(io.tolgee.constants.Message.INVALID_TIMESTAMP)
    }
  }

  fun isTimeStampValid(
    entityUnique: String,
    encryptedTimestamp: String?,
    maxAgeInMs: Long = tolgeeProperties.authentication.securedImageTimestampMaxAge
  ): Boolean {
    val timestampDecoded = decryptTimeStamp(encryptedTimestamp ?: return false)
    val now = Date().time
    if (entityUnique != timestampDecoded?.entityUnique) {
      return false
    }
    return now - maxAgeInMs <= (timestampDecoded.timestamp)
  }

  fun encryptTimeStamp(entityUnique: String, timestamp: Long): String {
    val byteArray = "$timestamp--$entityUnique".toByteArray(charset("ASCII"))
    return Hex.encodeHexString(aes.encrypt(byteArray))
  }

  fun decryptTimeStamp(encrypted: String): DecryptedTimestamp? {
    val byteArray = Hex.decodeHex(encrypted)
    val decrypted = aes.decrypt(byteArray)
    val timestampString = decrypted.toString(charset("ASCII"))
    val data = timestampString.split("--")
    if (data.size != 2) {
      return null
    }
    val (timestamp, entityUnique) = data
    return DecryptedTimestamp(timestamp.toLong(), entityUnique)
  }

  data class DecryptedTimestamp(
    val timestamp: Long,
    val entityUnique: String,
  )
}
