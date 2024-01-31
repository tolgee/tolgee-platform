package io.tolgee.formats.po.out

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
import io.tolgee.formats.IcuMessageEscapeRemover
import io.tolgee.formats.pluralData.PluralData
import io.tolgee.formats.po.FromIcuParamConvertor
import io.tolgee.formats.po.getLocaleFromTag
import io.tolgee.formats.po.getPluralDataOrNull
import io.tolgee.service.export.exporters.ConversionResult

class BaseIcuMessageToPoConvertor(
  val message: String,
  val argumentConverter: FromIcuParamConvertor,
  val languageTag: String = "en",
) {
  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private lateinit var tree: MessageNode

  private val locale: ULocale by lazy {
    getLocaleFromTag(languageTag)
  }

  private val languagePluralData by lazy {
    getPluralDataOrNull(locale) ?: let {
      PluralData.DATA["en"]!!
    }
  }

  private fun addToResult(
    value: String,
    keyword: String? = null,
  ) {
    val unescaped = IcuMessageEscapeRemover(value, keyword != null).escapeRemoved
    if (keyword == null) {
      otherResult.append(unescaped)
      pluralFormsResult?.values?.forEach { it.append(unescaped) }
      return
    }

    if (pluralFormsResult == null) {
      pluralFormsResult = mutableMapOf()
    }

    pluralFormsResult?.compute(keyword) { _, v ->
      (v ?: StringBuilder(otherResult)).append(unescaped)
    }

    if (keyword == OTHER_KEYWORD) {
      otherResult.append(unescaped)
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
        addToResult(argumentConverter.convert(node), form)
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
