package io.tolgee.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import org.springframework.stereotype.Component

@Component
class KeyCustomValuesValidator(
  val objectMapper: ObjectMapper,
) {
  fun validate(customData: Map<String, Any?>) {
    validate(objectMapper.writeValueAsString(customData))
  }

  fun validate(customDataJsonString: String?) {
    if (customDataJsonString.isNullOrBlank()) {
      return
    }

    if (customDataJsonString.length > 5000) {
      throw BadRequestException(Message.CUSTOM_VALUES_JSON_TOO_LONG)
    }
  }

  fun isValid(customData: Map<String, Any?>): Boolean {
    return try {
      validate(customData)
      true
    } catch (e: BadRequestException) {
      false
    }
  }
}
