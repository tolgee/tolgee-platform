package io.tolgee.service.export.exporters

import com.ibm.icu.text.MessagePattern.ArgType
import com.ibm.icu.text.MessagePatternUtil
import com.ibm.icu.text.MessagePatternUtil.ArgNode
import com.ibm.icu.text.MessagePatternUtil.MessageContentsNode
import com.ibm.icu.text.MessagePatternUtil.MessageNode
import com.ibm.icu.text.MessagePatternUtil.TextNode
import com.ibm.icu.text.PluralRules
import com.ibm.icu.text.PluralRules.FixedDecimal
import com.ibm.icu.util.ULocale
import io.tolgee.constants.Message
import io.tolgee.service.dataImport.processors.messageFormat.data.PluralData

class BaseIcuMessageToPoConvertor(
  val message: String,
  val argumentConverter: (ArgNode) -> String = { "%s" },
  val languageTag: String = "en",
) {
  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private lateinit var tree: MessageNode

  private val locale: ULocale by lazy {
    ULocale.forLanguageTag(languageTag)
  }

  private val languagePluralData by lazy {
    PluralData.DATA[locale.language] ?: let {
      warnings.add(Message.PLURAL_FORMS_NOT_FOUND_FOR_LANGUAGE to listOf(languageTag))
      PluralData.DATA["en"]!!
    }
  }

  private fun addToResult(
    value: String,
    keyword: String? = null,
  ) {
    if (keyword == null) {
      otherResult.append(value)
      pluralFormsResult?.values?.forEach { it.append(value) }
      return
    }

    if (pluralFormsResult == null) {
      pluralFormsResult = mutableMapOf()
    }

    pluralFormsResult?.compute(keyword) { _, v ->
      (v ?: StringBuilder(otherResult)).append(value)
    }

    if (keyword == OTHER_KEYWORD) {
      otherResult.append(value)
    }
  }

  /**
   * We need to store all plural forms
   */
  private var pluralFormsResult: MutableMap<String, StringBuilder>? = null

  private var otherResult = StringBuilder()

  private val warnings = mutableListOf<Pair<Message, List<String>>>()

  fun convert(): ConversionResult {
    tree = MessagePatternUtil.buildMessageNode(message)
    handleNode(tree)

    if (pluralFormsResult == null) {
      return ConversionResult(otherResult.toString(), null, warnings)
    }

    return getPluralResult()
  }

  private fun getPluralResult(): ConversionResult {
    val forms = pluralForms.map { it.value to it.key }.toMap()
    val plurals =
      languagePluralData.examples.map {
        val form = forms[it.plural] ?: OTHER_KEYWORD
        it.plural to (pluralFormsResult!![form] ?: otherResult)
      }.sortedBy { it.first }.map { it.second.toString() }.toList()

    return ConversionResult(null, plurals, warnings)
  }

  private fun handleNode(
    node: MessagePatternUtil.Node?,
    form: String? = null,
  ) {
    when (node) {
      is ArgNode -> {
        handleArgNode(node, form)
      }

      is TextNode -> {
        addToResult(node.text, form)
      }

      is MessageNode -> {
        node.contents.forEach {
          handleNode(it, form)
        }
      }

      is MessageContentsNode -> {
        if (node.type == MessageContentsNode.Type.REPLACE_NUMBER) {
          addToResult("%d", form)
        }
      }

      else -> {
      }
    }
  }

  private fun handleArgNode(
    node: ArgNode,
    form: String?,
  ) {
    when (node.argType) {
      ArgType.SIMPLE, ArgType.NONE -> {
        addToResult(argumentConverter(node), form)
      }

      ArgType.PLURAL -> {
        if (form != null) {
          warnings.add(Message.NESTED_PLURALS_NOT_SUPPORTED to listOf(node.toString()))
          addToResult(node.toString(), form)
        }
        handlePlural(node)
      }

      else -> {
        addToResult(node.toString())
        warnings.add(Message.ADVANCED_PARAMS_NOT_SUPPORTED to listOf(node.toString()))
      }
    }
  }

  private val pluralForms: Map<String, Int>
    get() {
      pluralFormsResult ?: return emptyMap()
      val pluralIndexes =
        pluralFormsResult!!
          .map { it.key to getPluralIndexesForKeyword(it.key) }.toMap()

      val allIndexes = pluralIndexes.flatMap { it.value }.toSet()
      return allIndexes.mapNotNull { index ->
        val keyword =
          pluralIndexes.entries
            // We need to find keyword which contains only this index, because "other" keyword matches all
            .find { entry -> entry.value.contains(index) && entry.value.size == 1 }?.key
            ?: pluralIndexes.entries.find { entry ->
              entry.value.contains(index)
            }?.key ?: return@mapNotNull null
        keyword to index
      }.toMap()
    }

  private fun getPluralIndexesForKeyword(keyword: String) =
    languagePluralData.examples.filter {
      // This is probably only way how to do it, so we have to use internal API
      @Suppress("DEPRECATION")
      PluralRules.forLocale(locale).matches(FixedDecimal(it.sample.toString()), keyword)
    }.map { it.plural }

  private fun handlePlural(node: ArgNode) {
    node.complexStyle.variants.forEach {
      handleNode(it.message, it.selector)
    }
  }
}
