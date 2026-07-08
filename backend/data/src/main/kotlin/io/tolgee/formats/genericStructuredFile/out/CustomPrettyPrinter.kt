package io.tolgee.formats.genericStructuredFile.out

import tools.jackson.core.JsonGenerator
import tools.jackson.core.util.DefaultPrettyPrinter

class CustomPrettyPrinter : DefaultPrettyPrinter() {
  override fun createInstance(): DefaultPrettyPrinter {
    return CustomPrettyPrinter()
  }

  override fun writeObjectNameValueSeparator(jg: JsonGenerator) {
    jg.writeRaw(": ")
  }
}
