package io.tolgee.formats

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.escaping.IcuUnescper
import io.tolgee.formats.escaping.PluralFormIcuEscaper
import io.tolgee.util.nullIfEmpty

val allPluralKeywords =
  listOf(
    PluralRules.KEYWORD_ZERO,
    PluralRules.KEYWORD_ONE,
    PluralRules.KEYWORD_TWO,
    PluralRules.KEYWORD_FEW,
    PluralRules.KEYWORD_MANY,
    PluralRules.KEYWORD_OTHER,
  )

fun getPluralFormsForLocale(languageTag: String): MutableSet<String> {
  val uLocale = getULocaleFromTag(languageTag)
  val pluralRules = PluralRules.forLocale(uLocale)
  return pluralRules.keywords.sortedBy {
    formKeywords.indexOf(it)
  }.toMutableSet()
}

fun populateForms(
  languageTag: String,
  forms: Map<String, String>,
): Map<String, String> {
  val otherForm = forms["other"] ?: ""
  val allForms = getPluralFormsForLocale(languageTag)
  return allForms.associateWith { (forms[it] ?: otherForm) }
}

fun orderPluralForms(pluralForms: Map<String, String>): Map<String, String> {
  return pluralForms.entries.sortedBy {
    val formIndex = formKeywords.indexOf(it.key)
    if (formIndex == -1) {
      "A_$it"
    } else {
      formIndex.toString()
    }
  }.associate { it.key to it.value }
}

val formKeywords = listOf("zero", "one", "two", "few", "many", "other")

/**
 * It takes the plurals and optimizes them by removing the unnecessary forms
 * It also sorts it, so the strings can be compared and should be the same for the same forms
 */
fun optimizePossiblePlural(string: String): String {
  val forms = getPluralForms(string) ?: return string
  val optimizedForms = optimizePluralForms(forms.forms)
  return FormsToIcuPluralConvertor(
    optimizedForms,
    addNewLines = true,
    argName = forms.argName,
  ).convert()
}

/**
 * Returns all plural forms from the given ICU string
 * Returns null if the string is not a plural
 */
fun getPluralForms(string: String?): PluralForms? {
  string ?: return null
  val converted = convertIcuStringNoOp(string)
  return getPluralFormsFromConversionResult(converted)
}

/**
 * Returns all plural forms from the given ICU string
 * Returns null if the string is not a plural
 */
fun getPluralFormsReplacingReplaceParam(
  string: String,
  replacement: String,
): PluralForms? {
  val noOpConvertor = NoOpFromIcuPlaceholderConvertor()
  val convertor = {
    object : FromIcuPlaceholderConvertor {
      override fun convert(node: MessagePatternUtil.ArgNode): String {
        return noOpConvertor.convert(node)
      }

      override fun convertText(
        node: MessagePatternUtil.TextNode,
        keepEscaping: Boolean,
      ): String {
        return noOpConvertor.convertText(node, keepEscaping)
      }

      override fun convertReplaceNumber(
        node: MessagePatternUtil.MessageContentsNode,
        argName: String?,
      ): String {
        return replacement
      }
    }
  }
  val converted =
    BaseIcuMessageConvertor(
      string,
      convertor,
      keepEscaping = true,
    ).convert()
  return getPluralFormsFromConversionResult(converted)
}

private fun getPluralFormsFromConversionResult(converted: PossiblePluralConversionResult): PluralForms? {
  return PluralForms(
    converted.formsResult ?: return null,
    converted.argName ?: throw IllegalStateException("Plural argument name not found"),
  )
}

data class PluralForms(
  val forms: Map<String, String>,
  val argName: String,
) {
  val icuString: String
    get() = forms.toIcuPluralString(optimize = false, argName = argName)
}

fun optimizePluralForms(forms: Map<String, String>): Map<String, String> {
  val otherForm = forms[PluralRules.KEYWORD_OTHER] ?: return forms
  val filtered =
    forms.filter { it.key == PluralRules.KEYWORD_OTHER || (it.value != otherForm && it.value.isNotEmpty()) }
  return orderPluralForms(filtered)
}

/**
 * It takes the plurals and optimizes them by removing the unnecessary forms
 * It also sorts it, so the strings can be compared and should be the same for the same forms
 */
infix fun String?.isSamePossiblePlural(other: String?): Boolean {
  if (this == other) {
    return true
  }
  if (this == null || other == null) {
    return false
  }
  return optimizePossiblePlural(this) == optimizePossiblePlural(other)
}

fun String?.convertToIcuPlural(newPluralArgName: String?): String? {
  if (this == null) {
    return null
  }
  return mapOf(1 to this).convertToIcuPlurals(newPluralArgName).convertedStrings[1]
}

/**
 * Converts map of strings to ICU plurals
 * If value is null, result is also null
 */
fun <T> Map<T, String?>.convertToIcuPlurals(newPluralArgName: String?): ConvertToIcuPluralResult<T> {
  val possibleArgNames = mutableListOf<String>()
  val invalid = mutableSetOf<T>()
  val formResults =
    this.map { entry ->
      entry.value ?: return@map entry.key to null
      entry.key to (
        try {
          val value = entry.value ?: return@map entry.key to null
          val converted =
            convertIcuStringNoOp(value)

          (converted.argName ?: converted.firstArgName)?.let {
            possibleArgNames.add(it)
          }

          converted.formsResult
        } catch (e: Exception) {
          null
        } ?: let {
          invalid.add(entry.key)
          mapOf("other" to entry.value!!)
        }
      )
    }.toMap()

  val argName = getArgName(possibleArgNames, newPluralArgName)
  val convertedStrings =
    formResults.map { (key, forms) ->
      val preparedForms = forms?.preparePluralForms(escapeHash = invalid.contains(key))
      key to preparedForms.preparedFormsToIcuPlural(argName)
    }.toMap()
  return ConvertToIcuPluralResult(convertedStrings, argName)
}

private fun convertIcuStringNoOp(string: String) =
  BaseIcuMessageConvertor(
    string,
    { IcuToIcuPlaceholderConvertor() },
    keepEscaping = true,
  ).convert()

data class ConvertToIcuPluralResult<T>(
  val convertedStrings: Map<T, String?>,
  val argName: String,
)

/**
 * Normalizes list of plurals. Uses provided argument name if any, otherwise it tries to find the most common one
 */
fun <T> normalizePlurals(
  strings: Map<T, String?>,
  pluralArgName: String? = null,
): Map<T, String?> {
  val invalidStrings = mutableListOf<String>()
  val formResults =
    strings.map {
      val text = it.value?.nullIfEmpty ?: return@map it.key to null

      val forms =
        try {
          getPluralForms(text)
        } catch (e: Exception) {
          null
        }

      if (forms == null) {
        invalidStrings.add(text)
      }

      it.key to forms
    }.toMap()

  if (invalidStrings.isNotEmpty()) {
    throw StringIsNotPluralException(invalidStrings)
  }

  return pluralFormsToSameArgName(formResults, pluralArgName).convertedStrings
}

/**
 * This method is useful when Support for ICU is disabled on project level and we
 * store such plural strings with escaped forms.
 *
 * Returns null if the string is not a plural
 */
fun String.forceEscapePluralForms(): MessageConvertorResult? {
  val forms = getPluralForms(this)
  val escapedForms = forms?.forms?.mapValues { ForceIcuEscaper(it.value, escapeHash = true).escaped }
  val text = escapedForms?.toIcuPluralString(optimize = false, argName = forms.argName) ?: return null
  return MessageConvertorResult(text, forms.argName)
}

/**
 * This method is useful when Support for ICU is disabled on project level and we
 * store such plural strings with escaped forms.
 *
 * Returns null if the string is not a plural
 */
fun String.unescapePluralForms(): String? {
  val forms = getPluralForms(this)
  val unescaped = forms?.forms?.mapValues { IcuUnescper(it.value, isPlural = true).unescaped }
  return unescaped?.toIcuPluralString(optimize = false, argName = forms.argName)
}

/**
 * Convert plurals to the same argument name
 */
private fun <T> pluralFormsToSameArgName(
  formResults: Map<T, PluralForms?>,
  pluralArgName: String?,
): ConvertToIcuPluralResult<T> {
  val argName = getArgName(formResults.values, pluralArgName)
  val convertedStrings =
    formResults.map { (key, forms) ->
      val preparedForms = forms?.forms?.preparePluralForms()
      key to preparedForms.preparedFormsToIcuPlural(argName)
    }.toMap()
  return ConvertToIcuPluralResult(convertedStrings, argName)
}

private fun Map<String, String>.preparePluralForms(escapeHash: Boolean = false): Map<String, String> {
  return this.mapValues {
    it.value.preparePluralForm(escapeHash)
  }
}

private fun Map<String, String>?.preparedFormsToIcuPlural(argName: String): String? {
  return this?.let {
    FormsToIcuPluralConvertor(
      it,
      addNewLines = true,
      argName = argName,
    ).convert()
  }
}

fun Map<String, String>.toIcuPluralString(
  optimize: Boolean = true,
  addNewLines: Boolean = true,
  argName: String,
): String {
  return FormsToIcuPluralConvertor(
    this,
    optimize = optimize,
    addNewLines = addNewLines,
    argName = argName,
  ).convert()
}

class StringIsNotPluralException(val invalidStrings: List<String>) : RuntimeException("String is not a plural")

private fun String.preparePluralForm(escapeHash: Boolean = false): String {
  return try {
    val result = StringBuilder()
    MessagePatternUtil.buildMessageNode(this).contents.forEach {
      if (it !is MessagePatternUtil.TextNode) {
        result.append(it.patternString)
        return@forEach
      }
      result.append(PluralFormIcuEscaper(it.patternString, escapeHash = escapeHash).escaped)
    }
    result.toString()
  } catch (e: Exception) {
    PluralFormIcuEscaper(this, escapeHash).escaped
  }
}

fun String.isPluralString(): Boolean {
  return try {
    getPluralForms(this)?.forms != null
  } catch (e: Exception) {
    false
  }
}

/**
 * Returns new map with plural forms if any of the values is plural
 * Returns null if none of the values is plural
 */
fun <T> Map<T, String?>.convertToPluralIfAnyIsPlural(): ConvertToIcuPluralResult<T>? {
  val shouldBePlural = this.any { it.value?.isPluralString() == true }
  if (!shouldBePlural) {
    return null
  }

  return this.convertToIcuPlurals(null)
}

/**
 * Returns provided argument name witn max count
 */
private fun getArgName(
  forms: Collection<PluralForms?>,
  pluralArgName: String?,
): String {
  return getArgName(forms.mapNotNull { it?.argName }, pluralArgName)
}

/**
 * Returns provided argument name witn max count
 */
private fun getArgName(
  possibleArgNames: List<String>,
  pluralArgName: String?,
): String {
  if (!pluralArgName.isNullOrBlank()) {
    return pluralArgName
  }

  val possibleArgNameSet = possibleArgNames.toSet()
  return possibleArgNameSet.map { argName ->
    argName to
      possibleArgNames.count { it == argName }
  }.maxByOrNull { it.second }?.first ?: DEFAULT_PLURAL_ARGUMENT_NAME
}
