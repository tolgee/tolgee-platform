package io.tolgee.service.dataImport.processors.messageFormat

enum class SupportedFormat(val poFlag: String) {
  PHP(poFlag = "php-format"),
  C(poFlag = "c-format"),
  PYTHON(poFlag = "python-format"),
  ;

  companion object {
    fun findByFlag(poFlag: String) = values().find { it.poFlag == poFlag }
  }
}
