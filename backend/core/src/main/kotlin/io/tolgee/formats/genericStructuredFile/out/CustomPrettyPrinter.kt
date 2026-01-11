package io.tolgee.formats.genericStructuredFile.out

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter

class CustomPrettyPrinter : DefaultPrettyPrinter() {
  override fun createInstance(): DefaultPrettyPrinter {
    return CustomPrettyPrinter()
  }

  override fun writeObjectFieldValueSeparator(jg: JsonGenerator) {
    jg.writeRaw(": ")
  }
}
