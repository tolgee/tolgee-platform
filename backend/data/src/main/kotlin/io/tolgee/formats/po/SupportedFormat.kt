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
  val exportMessageConverter: (message: String, languageTag: String, forceIsPlural: Boolean?) -> ToPoMessageConverter,
) {
  PHP(
    poFlag = "php-format",
    paramConvertorFactory = { PhpToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToPhpPoMessageConverter(
        message,
        languageTag,
        forceIsPlural,
      )
    },
  ),
  C(
    poFlag = "c-format",
    paramConvertorFactory = { CToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToCPoMessageConverter(
        message,
        languageTag,
        forceIsPlural,
      )
    },
  ),
  PYTHON(
    poFlag = "python-format",
    paramConvertorFactory = { PythonToIcuParamConvertor() },
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToPythonPoMessageConverter(
        message,
        languageTag,
        forceIsPlural,
      )
    },
  ),

  ;

  companion object {
    fun findByFlag(poFlag: String) = entries.find { it.poFlag == poFlag }
  }
}
