package io.tolgee.formats.po

import io.tolgee.formats.ImportMessageConvertorType
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.po.out.ToPoMessageConvertor
import io.tolgee.formats.po.out.c.ToCPoMessageConvertor
import io.tolgee.formats.po.out.php.ToPhpPoMessageConvertor

enum class PoSupportedMessageFormat(
  val poFlag: String,
  val paramRegex: Regex,
  val importMessageConvertorType: ImportMessageConvertorType,
  val exportMessageConverter: (
    message: String,
    languageTag: String,
    forceIsPlural: Boolean,
    projectIcuPlaceholdersSupport: Boolean,
  ) -> ToPoMessageConvertor,
) {
  PHP(
    poFlag = "php-format",
    importMessageConvertorType = ImportMessageConvertorType.PO_PHP,
    paramRegex = PhpToIcuPlaceholderConvertor.PHP_PARAM_REGEX,
    exportMessageConverter = { message, languageTag, forceIsPlural, projectIcuPlaceholdersSupport ->
      ToPhpPoMessageConvertor(
        message,
        languageTag,
        forceIsPlural,
        projectIcuPlaceholdersSupport,
      )
    },
  ),
  C(
    poFlag = "c-format",
    importMessageConvertorType = ImportMessageConvertorType.PO_C,
    paramRegex = CToIcuPlaceholderConvertor.C_PARAM_REGEX,
    exportMessageConverter = { message, languageTag, forceIsPlural, projectIcuPlaceholdersSupport ->
      ToCPoMessageConvertor(
        message,
        languageTag,
        forceIsPlural,
        projectIcuPlaceholdersSupport,
      )
    },
  ),
//  PYTHON(
//    poFlag = "python-format",
//    importMessageConvertorType = ImportMessageConvertorType.PO_PYTHON,
//    paramRegex = PythonToIcuParamConvertor.PYTHON_PARAM_REGEX,
//    exportMessageConverter = { message, languageTag, forceIsPlural, projectIcuPlaceholdersSupport ->
//      ToPythonPoMessageConvertor(
//        message,
//        languageTag,
//        forceIsPlural,
//        projectIcuPlaceholdersSupport,
//      )
//    },
//  ),

  ;

  companion object {
    fun findByFlag(poFlag: String) = entries.find { it.poFlag == poFlag }
  }
}
