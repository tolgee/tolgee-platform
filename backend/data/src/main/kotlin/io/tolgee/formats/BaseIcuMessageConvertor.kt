package io.tolgee.formats

import com.ibm.icu.text.MessagePattern
import io.tolgee.constants.Message

class BaseIcuMessageConvertor(
  private val message: String,
  private val argumentConverter: FromIcuParamConvertor,
) {
  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private var pluralArgName: String? = null

  private lateinit var tree: MessagePatternUtil.MessageNode

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
      is MessagePatternUtil.ArgNode -> {
        handleArgNode(node, form)
      }

      is MessagePatternUtil.TextNode -> {
        addToResult(node.text, form)
      }

      is MessagePatternUtil.MessageNode -> {
        node.contents.forEach {
          handleNode(it, form)
        }
      }

      is MessagePatternUtil.MessageContentsNode -> {
        if (node.type == MessagePatternUtil.MessageContentsNode.Type.REPLACE_NUMBER) {
          addToResult(argumentConverter.convertReplaceNumber(node, pluralArgName), form)
        }
      }

      else -> {
      }
    }
  }

  private fun handleArgNode(
    node: MessagePatternUtil.ArgNode,
    form: String?,
  ) {
    when (node.argType) {
      MessagePattern.ArgType.SIMPLE, MessagePattern.ArgType.NONE -> {
        val isInPlural = form != null
        addToResult(argumentConverter.convert(node, isInPlural), form)
      }

      MessagePattern.ArgType.PLURAL -> {
        if (form != null) {
          warnings.add(Message.NESTED_PLURALS_NOT_SUPPORTED to listOf(node.patternString))
          addToResult(node.patternString, form)
          return
        }
        handlePlural(node)
      }

      else -> {
        addToResult(node.patternString, form)
        warnings.add(Message.ADVANCED_PARAMS_NOT_SUPPORTED to listOf(node.patternString))
      }
    }
  }

  private fun handlePlural(node: MessagePatternUtil.ArgNode) {
    pluralArgName = node.name
    node.complexStyle?.variants?.forEach {
      handleNode(it.message, it.selector)
    } ?: run {
      addToResult(node.patternString)
    }
  }
}
