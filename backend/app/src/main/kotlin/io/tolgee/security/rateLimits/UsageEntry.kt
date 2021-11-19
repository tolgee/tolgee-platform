package io.tolgee.security.rateLimits

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.*

data class UsageEntry(
  var time: Date,
  var availableTokens: Int,
) {
  fun serialize(): String {
    return jacksonObjectMapper().writeValueAsString(this)
  }

  companion object {
    fun deserialize(str: String): UsageEntry? {
      return try {
        jacksonObjectMapper().readValue(str)
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    }
  }
}
