package io.tolgee.formats.po

import io.tolgee.formats.ImportMessageConvertorType
import io.tolgee.formats.po.`in`.paramConvertors.CToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor
import io.tolgee.formats.po.`in`.paramConvertors.PythonToIcuParamConvertor
import io.tolgee.formats.po.out.ToPoMessageConvertor
import io.tolgee.formats.po.out.c.ToCPoMessageConvertor
import io.tolgee.formats.po.out.php.ToPhpPoMessageConvertor
import io.tolgee.formats.po.out.python.ToPythonPoMessageConvertor

enum class PoSupportedMessageFormat(
  val poFlag: String,
  val paramRegex: Regex,
  val importMessageConvertorType: ImportMessageConvertorType,
  val exportMessageConverter: (message: String, languageTag: String, forceIsPlural: Boolean?) -> ToPoMessageConvertor,
) {
  PHP(
    poFlag = "php-format",
    importMessageConvertorType = ImportMessageConvertorType.PO_PHP,
    paramRegex = PhpToIcuParamConvertor.PHP_PARAM_REGEX,
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToPhpPoMessageConvertor(
        message,
        languageTag,
        forceIsPlural,
      )
    },
  ),
  C(
    poFlag = "c-format",
    importMessageConvertorType = ImportMessageConvertorType.PO_C,
    paramRegex = CToIcuParamConvertor.C_PARAM_REGEX,
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToCPoMessageConvertor(
        message,
        languageTag,
        forceIsPlural,
      )
    },
  ),
  PYTHON(
    poFlag = "python-format",
    importMessageConvertorType = ImportMessageConvertorType.PO_PYTHON,
    paramRegex = PythonToIcuParamConvertor.PYTHON_PARAM_REGEX,
    exportMessageConverter = { message, languageTag, forceIsPlural ->
      ToPythonPoMessageConvertor(
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
