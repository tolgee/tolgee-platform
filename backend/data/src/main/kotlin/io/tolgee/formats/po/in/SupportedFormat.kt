package io.tolgee.formats.po.`in`

import io.tolgee.formats.po.`in`.paramConvertors.CToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor
import io.tolgee.formats.po.out.php.ToPhpPoMessageConverter
import io.tolgee.formats.po.out.php.ToPoMessageConverter

enum class SupportedFormat(
  val poFlag: String,
  val paramConvertorFactory: () -> ToIcuParamConvertor,
  val exportMessageConverter: (message: String, languageTag: String) -> ToPoMessageConverter,
) {
  PHP(
    poFlag = "php-format",
    paramConvertorFactory = { PhpToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag -> ToPhpPoMessageConverter(message, languageTag) },
  ),
  C(
    poFlag = "c-format",
    paramConvertorFactory = { CToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag -> kotlin.TODO() },
  ),
  PYTHON(
    poFlag = "python-format",
    paramConvertorFactory = { PythonToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag -> kotlin.TODO() },
  ),

  ;

  companion object {
    fun findByFlag(poFlag: String) = entries.find { it.poFlag == poFlag }
  }
}
