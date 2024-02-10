package io.tolgee.formats

import com.ibm.icu.text.MessagePattern.ArgType
import com.ibm.icu.text.MessagePatternUtil
import com.ibm.icu.text.MessagePatternUtil.ArgNode
import com.ibm.icu.text.MessagePatternUtil.MessageContentsNode
import com.ibm.icu.text.MessagePatternUtil.MessageNode
import com.ibm.icu.text.MessagePatternUtil.TextNode
import io.tolgee.constants.Message
import io.tolgee.formats.po.FromIcuParamConvertor

class BaseIcuMessageConvertor(
  private val message: String,
  private val argumentConverter: FromIcuParamConvertor,
) {
  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private var pluralArgName: String? = null

  private lateinit var tree: MessageNode

  private fun addToResult(
    value: String,
    keyword: String? = null,
  ) {
    val unescaped =
      IcuMessageEscapeRemover(value, keyword != null)
        .escapeRemoved
    if (keyword == null) {
      singleResult.append(unescaped)
      pluralFormsResult?.values?.forEach { it.append(unescaped) }
      otherResult?.append(unescaped)
      return
    }

    if (pluralFormsResult == null) {
      pluralFormsResult = mutableMapOf()
    }

    pluralFormsResult?.compute(keyword) { _, v ->
      (v ?: StringBuilder(singleResult)).append(unescaped)
    }

    if (keyword == OTHER_KEYWORD) {
      if (otherResult == null) {
        otherResult = StringBuilder(singleResult)
      }
      otherResult!!.append(unescaped)
    }
  }

  /**
   * We need to store all plural forms
   */
  private var pluralFormsResult: MutableMap<String, StringBuilder>? = null

  private var otherResult: StringBuilder? = null
  private var singleResult = StringBuilder()

  private val warnings = mutableListOf<Pair<Message, List<String>>>()

  fun convert(): PossiblePluralConversionResult {
    tree = MessagePatternUtil.buildMessageNode(message)
    handleNode(tree)

    if (pluralFormsResult == null) {
      return PossiblePluralConversionResult(singleResult.toString(), null, warnings)
    }

    return getPluralResult()
  }

  private fun getPluralResult(): PossiblePluralConversionResult {
    val result =
      pluralFormsResult
        ?.mapValues { it.value.toString() }
        ?.toMutableMap() ?: mutableMapOf()
    result.computeIfAbsent(OTHER_KEYWORD) { otherResult.toString() }
    return PossiblePluralConversionResult(null, result, warnings)
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
          addToResult(argumentConverter.convertReplaceNumber(node, pluralArgName), form)
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
        val isInPlural = form != null
        addToResult(argumentConverter.convert(node, isInPlural), form)
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

  private fun handlePlural(node: ArgNode) {
    pluralArgName = node.name
    node.complexStyle.variants.forEach {
      handleNode(it.message, it.selector)
    }
  }
}
