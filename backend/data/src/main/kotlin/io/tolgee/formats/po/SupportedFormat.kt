package io.tolgee.formats.po

import io.tolgee.formats.po.`in`.PythonToIcuParamConvertor
import io.tolgee.formats.po.`in`.ToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.CToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor
import io.tolgee.formats.po.out.ToPoMessageConverter
import io.tolgee.formats.po.out.c.ToCPoMessageConverter
import io.tolgee.formats.po.out.php.ToPhpPoMessageConverter
import io.tolgee.formats.po.out.python.ToPythonPoMessageConverter

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
    exportMessageConverter = { message, languageTag -> ToCPoMessageConverter(message, languageTag) },
  ),
  PYTHON(
    poFlag = "python-format",
    paramConvertorFactory = { PythonToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag -> ToPythonPoMessageConverter(message, languageTag) },
  ),

  ;

  companion object {
    fun findByFlag(poFlag: String) = entries.find { it.poFlag == poFlag }
  }
}
