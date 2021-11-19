package io.tolgee.dtos.dataImport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class ImportStreamingProgressMessage(
  val type: ImportStreamingProgressMessageType,
  val params: List<Any?>? = null
) {
  fun toJsonByteArray(): ByteArray {
    return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this).toByteArray()
  }
}
