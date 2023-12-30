package io.tolgee.service.machineTranslation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class MetaMapper {
  fun getMeta(data: String): List<MetaKey>? {
    val mapper = jacksonObjectMapper()
    val typeRef = object : TypeReference<List<MetaKey>>() {}
    return mapper.readValue(data, typeRef)
  }

  companion object {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class MetaKey(
      val keyName: String,
      val namespace: String?,
    )
  }
}
