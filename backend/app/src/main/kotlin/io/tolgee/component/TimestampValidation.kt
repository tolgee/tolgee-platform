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
    timestamp: String,
    maxAgeInMs: Long = tolgeeProperties.authentication.securedImageTimestampMaxAge
  ) {
    if (!isTimeStampValid(timestamp, maxAgeInMs)) {
      throw ValidationException(io.tolgee.constants.Message.INVALID_TIMESTAMP)
    }
  }

  fun isTimeStampValid(
    encryptedTimestamp: String,
    maxAgeInMs: Long = tolgeeProperties.authentication.securedImageTimestampMaxAge
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
