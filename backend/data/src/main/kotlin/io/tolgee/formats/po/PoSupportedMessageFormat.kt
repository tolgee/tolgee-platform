package io.tolgee.formats.po

import io.tolgee.formats.MessageConvertorType
import io.tolgee.formats.po.`in`.paramConvertors.CToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PythonToIcuParamConvertor
import io.tolgee.formats.po.out.ToPoMessageConverter
import io.tolgee.formats.po.out.c.ToCPoMessageConverter
import io.tolgee.formats.po.out.php.ToPhpPoMessageConverter
import io.tolgee.formats.po.out.python.ToPythonPoMessageConverter

enum class PoSupportedMessageFormat(
  val poFlag: String,
  val paramRegex: Regex,
  val messageConvertorType: MessageConvertorType,
  val exportMessageConverter: (message: String, languageTag: String, forceIsPlural: Boolean?) -> ToPoMessageConverter,
) {
  PHP(
    poFlag = "php-format",
    messageConvertorType = MessageConvertorType.PO_PHP,
    paramRegex = PhpToIcuParamConvertor.PHP_PARAM_REGEX,
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
    messageConvertorType = MessageConvertorType.PO_C,
    paramRegex = CToIcuParamConvertor.C_PARAM_REGEX,
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
    messageConvertorType = MessageConvertorType.PO_PYTHON,
    paramRegex = PythonToIcuParamConvertor.PYTHON_PARAM_REGEX,
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
