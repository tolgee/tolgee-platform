package io.tolgee.formats

import com.ibm.icu.text.MessagePattern
import io.tolgee.constants.Message

class BaseIcuMessageConvertor(
  private val message: String,
  private val argumentConvertorFactory: () -> FromIcuPlaceholderConvertor,
  private val keepEscaping: Boolean = false,
  private val forceIsPlural: Boolean? = null,
) {
  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private var pluralArgName: String? = null

  private var firstArgName: String? = null

  private lateinit var tree: MessagePatternUtil.MessageNode

  private fun addToResult(
    value: String,
    keyword: String? = null,
  ) {
    if (keyword == null) {
      singleResult.append(value)
      pluralFormsResult?.values?.forEach { it.append(value) }
      otherResult?.append(value)
      return
    }

    if (pluralFormsResult == null) {
      pluralFormsResult = mutableMapOf()
    }

    pluralFormsResult?.compute(keyword) { _, v ->
      (v ?: StringBuilder(singleResult)).append(value)
    }

    if (keyword == OTHER_KEYWORD) {
      if (otherResult == null) {
        otherResult = StringBuilder(singleResult)
      }
      otherResult!!.append(value)
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
    return catchingCannotParse {
      tree = MessagePatternUtil.buildMessageNode(message)
      handleNode(tree)

      if ((pluralFormsResult == null && forceIsPlural != true) || forceIsPlural == false) {
        return@catchingCannotParse getSingularResult()
      }
      getPluralResult()
    }
  }

  private fun getSingularResult(): PossiblePluralConversionResult {
    return PossiblePluralConversionResult(
      singleResult.toString(),
      null,
      null,
      firstArgName = firstArgName,
    )
  }

  private fun catchingCannotParse(fn: () -> PossiblePluralConversionResult): PossiblePluralConversionResult {
    try {
      return fn()
    } catch (e: Exception) {
      if (forceIsPlural == true) {
        val escaped = message.escapeIcu(true)

        return PossiblePluralConversionResult(
          formsResult = mapOf("other" to escaped),
        )
      }
      return PossiblePluralConversionResult(
        singleResult = message,
      )
    }
  }

  private fun getPluralResult(): PossiblePluralConversionResult {
    val result =
      pluralFormsResult
        ?.mapValues { it.value.toString() }
        ?.toMutableMap() ?: mutableMapOf()

    val otherResult =
      if (forceIsPlural == true && otherResult == null) {
        singleResult
      } else {
        otherResult ?: ""
      }

    result.computeIfAbsent(OTHER_KEYWORD) { otherResult.toString() }
    return PossiblePluralConversionResult(
      null,
      result,
      pluralArgName,
      firstArgName = firstArgName,
    )
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
        appendFromTextNode(node, form)
      }

      is MessagePatternUtil.MessageNode -> {
        node.contents.forEach {
          handleNode(it, form)
        }
      }

      is MessagePatternUtil.MessageContentsNode -> {
        if (node.type == MessagePatternUtil.MessageContentsNode.Type.REPLACE_NUMBER) {
          addToResult(getFormPlaceholderConvertor(form).convertReplaceNumber(node, pluralArgName), form)
        }
      }

      else -> {
      }
    }
  }

  private fun appendFromTextNode(
    node: MessagePatternUtil.TextNode,
    form: String?,
  ) {
    val formPlaceholderConvertor = getFormPlaceholderConvertor(form)
    val convertedText = formPlaceholderConvertor.convertText(node, keepEscaping)
    addToResult(convertedText, form)
  }

  private fun handleArgNode(
    node: MessagePatternUtil.ArgNode,
    form: String?,
  ) {
    if (firstArgName == null) {
      firstArgName = node.name
    }
    when (node.argType) {
      MessagePattern.ArgType.SIMPLE, MessagePattern.ArgType.NONE -> {
        addToResult(getFormPlaceholderConvertor(form).convert(node), form)
      }

      MessagePattern.ArgType.PLURAL -> {
        if (forceIsPlural == false) {
          addToResult(node.patternString)
          return
        }

        if (!pluralFormsResult.isNullOrEmpty()) {
          warnings.add(Message.MULTIPLE_PLURALS_NOT_SUPPORTED to listOf(node.patternString))
          addToResult(node.patternString, form)
          return
        }
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

  private val formArgumentConvertor =
    mutableMapOf<String?, FromIcuPlaceholderConvertor>()

  private fun getFormPlaceholderConvertor(form: String?): FromIcuPlaceholderConvertor {
    return formArgumentConvertor.computeIfAbsent(form) { argumentConvertorFactory() }
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
