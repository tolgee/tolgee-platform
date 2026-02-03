package io.tolgee.util

import io.tolgee.exceptions.BadRequestException
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.lang.Integer.min
import java.util.Locale

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class SlugGenerator {
  companion object {
    const val DELIMITER = "-"
  }

  fun generate(
    name: String,
    minLength: Int,
    maxLength: Int,
    checkUniquenessCallback: (name: String) -> Boolean,
  ): String {
    var namePart =
      StringUtils
        .stripAccents(name)
        .lowercase(Locale.getDefault())
        .replace("[^a-z0-9]+".toRegex(), DELIMITER)
        .let { it.substring(0, min(it.length, maxLength)) }

    namePart = namePart.removePrefix(DELIMITER)

    var numPart = 0
    var tries = 0

    while (true) {
      if (tries > 5000) {
        throw BadRequestException(io.tolgee.constants.Message.CANNOT_FIND_SUITABLE_ADDRESS_PART)
      }
      tries++

      if ((namePart + numPart.emptyOnZero()).length > maxLength) {
        namePart = namePart.substring(0, namePart.length - 1)
      }

      namePart = namePart.removeSuffix("-")

      val result = namePart + numPart.emptyOnZero()
      if (result.length >= minLength && checkUniquenessCallback(result)) {
        // has to contain letter
        if (result.matches(".*[a-z]+.*+".toRegex())) {
          return result
        }
        namePart += "a"
        numPart = 0
      }

      numPart++
    }
  }

  private fun Int.emptyOnZero() = if (this == 0) "" else this.toString()
}
