package io.tolgee.formats

import com.ibm.icu.text.MessagePattern
import com.ibm.icu.text.MessagePatternUtil
import java.util.Collections
import kotlin.concurrent.Volatile

/**
 * Original license:
 * © 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 *******************************************************************************
 *   Copyright (C) 2011-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 *   created on: 2011jul14
 *   created by: Markus W. Scherer
 *
 *******************************************
 *
 * Tolgee docs:
 *
 * We took this file from ICU4J and added tools to propertly get part of original message from ICU message pattern.
 * We need this to reliable convert ICU message to plural forms.
 *
 ********************************************
 *
 * Original docs:
 *
 * Utilities for working with a MessagePattern.
 * Intended for use in tools when convenience is more important than
 * minimizing runtime and object creations.
 *
 *
 * This class only has static methods.
 * Each of the nested classes is immutable and thread-safe.
 *
 *
 * This class and its nested classes are not intended for public subclassing.
 * @stable ICU 49
 * @author Markus Scherer
 */
object MessagePatternUtil {
  /**
   * Factory method, builds and returns a MessageNode from a MessageFormat pattern string.
   * @param patternString a MessageFormat pattern string
   * @return a MessageNode or a ComplexArgStyleNode
   * @throws IllegalArgumentException if the MessagePattern is empty
   * or does not represent a MessageFormat pattern
   * @stable ICU 49
   */
  fun buildMessageNode(patternString: String?): MessageNode {
    return buildMessageNode(MessagePattern(patternString))
  }

  /**
   * Factory method, builds and returns a MessageNode from a MessagePattern.
   * @param pattern a parsed MessageFormat pattern string
   * @return a MessageNode or a ComplexArgStyleNode
   * @throws IllegalArgumentException if the MessagePattern is empty
   * or does not represent a MessageFormat pattern
   * @stable ICU 49
   */
  fun buildMessageNode(pattern: MessagePattern): MessageNode {
    val limit = pattern.countParts() - 1
    require(limit >= 0) { "The MessagePattern is empty" }
    require(pattern.getPartType(0) == MessagePattern.Part.Type.MSG_START) {
      "The MessagePattern does not represent a MessageFormat pattern"
    }
    return buildMessageNode(pattern, 0, limit)
  }

  private fun buildMessageNode(
    pattern: MessagePattern,
    start: Int,
    limit: Int,
  ): MessageNode {
    var prevPatternIndex = pattern.getPart(start).limit
    val node = MessageNode(pattern, start, limit)
    var i = start + 1
    while (true) {
      var part = pattern.getPart(i)
      val patternIndex = part.index
      val isSkipSyntax = part.type == MessagePattern.Part.Type.SKIP_SYNTAX
      if (prevPatternIndex < patternIndex) {
        val text =
          pattern.patternString.substring(
            prevPatternIndex,
            patternIndex,
          )

        node.addContentsNode(
          TextNode(pattern, text, text, start = i - 1, limit = i),
        )
      }

      if (isSkipSyntax) {
        node.addContentsNode(
          TextNode(pattern, "", "'", start = i - 1, limit = i),
        )
      }

      if (i == limit) {
        break
      }
      val partType = part.type
      if (partType == MessagePattern.Part.Type.ARG_START) {
        val argLimit = pattern.getLimitPartIndex(i)
        node.addContentsNode(buildArgNode(pattern, i, argLimit))
        i = argLimit
        part = pattern.getPart(i)
      } else if (partType == MessagePattern.Part.Type.REPLACE_NUMBER) {
        node.addContentsNode(MessageContentsNode.createReplaceNumberNode(pattern, i))
        // else: ignore SKIP_SYNTAX and INSERT_CHAR parts.
      }
      prevPatternIndex = part.limit
      ++i
    }
    return node.freeze()
  }

  private fun buildArgNode(
    pattern: MessagePattern,
    start: Int,
    limit: Int,
  ): ArgNode {
    var start = start
    val node: ArgNode = ArgNode.createArgNode(pattern, start, limit)
    var part = pattern.getPart(start)
    node.argType = part.argType
    val argType = node.argType
    part = pattern.getPart(++start) // ARG_NAME or ARG_NUMBER
    node.name = pattern.getSubstring(part)
    if (part.type == MessagePattern.Part.Type.ARG_NUMBER) {
      node.number = part.value
    }
    ++start
    when (argType) {
      MessagePattern.ArgType.SIMPLE -> {
        // ARG_TYPE
        node.typeName = pattern.getSubstring(pattern.getPart(start++))
        if (start < limit) {
          // ARG_STYLE
          node.style = pattern.getSubstring(pattern.getPart(start))
        }
      }

      MessagePattern.ArgType.CHOICE -> {
        node.typeName = "choice"
        node.complexStyle = buildChoiceStyleNode(pattern, start, limit)
      }

      MessagePattern.ArgType.PLURAL -> {
        node.typeName = "plural"
        node.complexStyle = buildPluralStyleNode(pattern, start, limit, argType)
      }

      MessagePattern.ArgType.SELECT -> {
        node.typeName = "select"
        node.complexStyle = buildSelectStyleNode(pattern, start, limit)
      }

      MessagePattern.ArgType.SELECTORDINAL -> {
        node.typeName = "selectordinal"
        node.complexStyle = buildPluralStyleNode(pattern, start, limit, argType)
      }

      else -> {}
    }
    return node
  }

  private fun buildChoiceStyleNode(
    pattern: MessagePattern,
    start: Int,
    limit: Int,
  ): ComplexArgStyleNode {
    var start = start
    val node = ComplexArgStyleNode(pattern, MessagePattern.ArgType.CHOICE, start, limit)
    while (start < limit) {
      val valueIndex = start
      val part = pattern.getPart(start)
      val value = pattern.getNumericValue(part)
      start += 2
      val msgLimit = pattern.getLimitPartIndex(start)
      val variant = VariantNode(pattern, start, msgLimit)
      variant.selector = pattern.getSubstring(pattern.getPart(valueIndex + 1))
      variant.numericValue = value
      variant.msgNode = buildMessageNode(pattern, start, msgLimit)
      node.addVariant(variant)
      start = msgLimit + 1
    }
    return node.freeze()
  }

  private fun buildPluralStyleNode(
    pattern: MessagePattern,
    start: Int,
    limit: Int,
    argType: MessagePattern.ArgType,
  ): ComplexArgStyleNode {
    var start = start
    val node = ComplexArgStyleNode(pattern, argType, start, limit)
    val offset = pattern.getPart(start)
    if (offset.type.hasNumericValue()) {
      node.explicitOffset = true
      node.offset = pattern.getNumericValue(offset)
      ++start
    }
    while (start < limit) {
      val selector = pattern.getPart(start++)
      var value = MessagePattern.NO_NUMERIC_VALUE
      val part = pattern.getPart(start)
      if (part.type.hasNumericValue()) {
        value = pattern.getNumericValue(part)
        ++start
      }
      val msgLimit = pattern.getLimitPartIndex(start)
      val variant = VariantNode(pattern, start, msgLimit)
      variant.selector = pattern.getSubstring(selector)
      variant.numericValue = value
      variant.msgNode = buildMessageNode(pattern, start, msgLimit)
      node.addVariant(variant)
      start = msgLimit + 1
    }
    return node.freeze()
  }

  private fun buildSelectStyleNode(
    pattern: MessagePattern,
    start: Int,
    limit: Int,
  ): ComplexArgStyleNode {
    var start = start
    val node = ComplexArgStyleNode(pattern, MessagePattern.ArgType.SELECT, start, limit)
    while (start < limit) {
      val selector = pattern.getPart(start++)
      val msgLimit = pattern.getLimitPartIndex(start)
      val variant = VariantNode(pattern, start, msgLimit)
      variant.selector = pattern.getSubstring(selector)
      variant.msgNode = buildMessageNode(pattern, start, msgLimit)
      node.addVariant(variant)
      start = msgLimit + 1
    }
    return node.freeze()
  }

  /**
   * Common base class for all elements in a tree of nodes
   * returned by [MessagePatternUtil.buildMessageNode].
   * This class and all subclasses are immutable and thread-safe.
   * @stable ICU 49
   */
  abstract class Node(
    protected val owningPattern: MessagePattern,
    val start: Int,
    val limit: Int,
  ) {
    open val patternString: String by lazy {
      val startPart = owningPattern.getPart(start).index
      val endPart = owningPattern.getPart(limit).limit
      owningPattern.patternString.subSequence(startPart, endPart).toString()
    }
  }

  /**
   * A Node representing a parsed MessageFormat pattern string.
   * @stable ICU 49
   */
  class MessageNode(
    val pattern: MessagePattern,
    start: Int,
    limit: Int,
  ) : Node(pattern, start, limit) {
    val contents: List<MessageContentsNode>
      /**
       * @return the list of MessageContentsNode nodes that this message contains
       * @stable ICU 49
       */
      get() = list

    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      return list.toString()
    }

    fun addContentsNode(node: MessageContentsNode) {
      if (node is TextNode && !list.isEmpty()) {
        // Coalesce adjacent text nodes.
        val lastNode = list[list.size - 1]
        if (lastNode is TextNode) {
          val textNode = lastNode
          textNode.text += node.text
          textNode.patternString += node.patternString
          return
        }
      }
      list.add(node)
    }

    fun freeze(): MessageNode {
      list = Collections.unmodifiableList(list)
      return this
    }

    @Volatile
    private var list: MutableList<MessageContentsNode> = ArrayList()
  }

  /**
   * A piece of MessageNode contents.
   * Use getType() to determine the type and the actual Node subclass.
   * @stable ICU 49
   */
  open class MessageContentsNode(
    /**
     * Returns the type of this piece of MessageNode contents.
     * @stable ICU 49
     */
    owningPattern: MessagePattern,
    val type: Type,
    start: Int,
    limit: Int,
  ) : Node(owningPattern, start, limit) {
    /**
     * The type of a piece of MessageNode contents.
     * @stable ICU 49
     */
    enum class Type {
      /**
       * This is a TextNode containing literal text (downcast and call getText()).
       * @stable ICU 49
       */
      TEXT,

      /**
       * This is an ArgNode representing a message argument
       * (downcast and use specific methods).
       * @stable ICU 49
       */
      ARG,

      /**
       * This Node represents a place in a plural argument's variant where
       * the formatted (plural-offset) value is to be put.
       * @stable ICU 49
       */
      REPLACE_NUMBER,
    }

    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      // Note: There is no specific subclass for REPLACE_NUMBER
      // because it would not provide any additional API.
      // Therefore we have a little bit of REPLACE_NUMBER-specific code
      // here in the contents-node base class.
      return "{REPLACE_NUMBER}"
    }

    companion object {
      fun createReplaceNumberNode(
        owningPattern: MessagePattern,
        start: Int,
      ): MessageContentsNode {
        return MessageContentsNode(owningPattern, Type.REPLACE_NUMBER, start, start)
      }
    }
  }

  /**
   * Literal text, a piece of MessageNode contents.
   * @stable ICU 49
   */
  class TextNode(
    owningPattern: MessagePattern,
    /**
     * @return the literal text at this point in the message
     * @stable ICU 49
     */
    var text: String,
    override var patternString: String,
    start: Int,
    limit: Int,
  ) : MessageContentsNode(owningPattern, Type.TEXT, start, limit) {
    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      return "«$text»"
    }

    fun getText(keepEscaping: Boolean): String {
      if (keepEscaping) {
        return this.patternString
      }
      return this.text
    }
  }

  /**
   * A piece of MessageNode contents representing a message argument and its details.
   * @stable ICU 49
   */
  class ArgNode private constructor(
    owningPattern: MessagePattern,
    start: Int,
    limit: Int,
  ) : MessageContentsNode(owningPattern, Type.ARG, start, limit) {
    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      val sb = StringBuilder()
      sb.append('{').append(name)
      if (argType != MessagePattern.ArgType.NONE) {
        sb.append(',').append(typeName)
        if (argType == MessagePattern.ArgType.SIMPLE) {
          if (simpleStyle != null) {
            sb.append(',').append(simpleStyle)
          }
        } else {
          sb.append(',').append(complexStyle.toString())
        }
      }
      return sb.append('}').toString()
    }

    var style: String? = null

    /**
     * @return the argument type
     * @stable ICU 49
     */
    var argType: MessagePattern.ArgType? = null

    /**
     * @return the argument name string (the decimal-digit string if the argument has a number)
     * @stable ICU 49
     */
    var name: String? = null

    /**
     * @return the argument number, or -1 if none (for a named argument)
     * @stable ICU 49
     */
    var number: Int = -1

    /**
     * @return the argument type string, or null if none was specified
     * @stable ICU 49
     */
    var typeName: String? = null

    /**
     * @return the simple-argument style string,
     * or null if no style is specified and for other argument types
     * @stable ICU 49
     */
    val simpleStyle: String
      get() = style ?: ""

    /**
     * @return the complex-argument-style object,
     * or null if the argument type is NONE_ARG or SIMPLE_ARG
     * @stable ICU 49
     */
    var complexStyle: ComplexArgStyleNode? = null

    companion object {
      fun createArgNode(
        owningPattern: MessagePattern,
        start: Int,
        limit: Int,
      ): ArgNode {
        return ArgNode(owningPattern, start, limit)
      }
    }
  }

  /**
   * A Node representing details of the argument style of a complex argument.
   * (Which is a choice/plural/select argument which selects among nested messages.)
   * @stable ICU 49
   */
  class ComplexArgStyleNode(
    owningPattern: MessagePattern,
    /**
     * @return the argument type (same as getArgType() on the parent ArgNode)
     * @stable ICU 49
     */
    val argType: MessagePattern.ArgType,
    start: Int,
    limit: Int,
  ) : Node(owningPattern, start, limit) {
    /**
     * @return true if this is a plural style with an explicit offset
     * @stable ICU 49
     */
    fun hasExplicitOffset(): Boolean {
      return explicitOffset
    }

    val variants: List<VariantNode>
      /**
       * @return the list of variants: the nested messages with their selection criteria
       * @stable ICU 49
       */
      get() = list

    /**
     * Separates the variants by type.
     * Intended for use with plural and select argument styles,
     * not useful for choice argument styles.
     *
     *
     * Both parameters are used only for output, and are first cleared.
     * @param numericVariants Variants with numeric-value selectors (if any) are added here.
     * Can be null for a select argument style.
     * @param keywordVariants Variants with keyword selectors, except "other", are added here.
     * For a plural argument, if this list is empty after the call, then
     * all variants except "other" have explicit values
     * and PluralRules need not be called.
     * @return the "other" variant (the first one if there are several),
     * null if none (choice style)
     * @stable ICU 49
     */
    fun getVariantsByType(
      numericVariants: MutableList<VariantNode?>?,
      keywordVariants: MutableList<VariantNode?>,
    ): VariantNode? {
      numericVariants?.clear()
      keywordVariants.clear()
      var other: VariantNode? = null
      for (variant in list) {
        if (variant.isSelectorNumeric) {
          numericVariants!!.add(variant)
        } else if ("other" == variant.selector) {
          if (other == null) {
            // Return the first "other" variant. (MessagePattern allows duplicates.)
            other = variant
          }
        } else {
          keywordVariants.add(variant)
        }
      }
      return other
    }

    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      val sb = StringBuilder()
      sb.append('(').append(argType.toString()).append(" style) ")
      if (hasExplicitOffset()) {
        sb.append("offset:").append(offset).append(' ')
      }
      return sb.append(list.toString()).toString()
    }

    fun addVariant(variant: VariantNode) {
      list.add(variant)
    }

    fun freeze(): ComplexArgStyleNode {
      list = Collections.unmodifiableList(list)
      return this
    }

    /**
     * @return the plural offset, or 0 if this is not a plural style or
     * the offset is explicitly or implicitly 0
     * @stable ICU 49
     */
    var offset: Double = 0.0
    var explicitOffset = false

    @Volatile
    private var list: MutableList<VariantNode> = ArrayList()
  }

  /**
   * A Node representing a nested message (nested inside an argument)
   * with its selection criterion.
   * @stable ICU 49
   */
  class VariantNode(
    owningPattern: MessagePattern,
    start: Int,
    limit: Int,
  ) : Node(owningPattern, start, limit) {
    val isSelectorNumeric: Boolean
      /**
       * @return true for choice variants and for plural explicit values
       * @stable ICU 49
       */
      get() = selectorValue != MessagePattern.NO_NUMERIC_VALUE

    /**
     * {@inheritDoc}
     * @stable ICU 49
     */
    override fun toString(): String {
      val sb = StringBuilder()
      if (isSelectorNumeric) {
        sb
          .append(selectorValue)
          .append(" (")
          .append(selector)
          .append(") {")
      } else {
        sb.append(selector).append(" {")
      }
      return sb.append(message.toString()).append('}').toString()
    }

    /**
     * Returns the selector string.
     * For example: A plural/select keyword ("few"), a plural explicit value ("=1"),
     * a choice comparison operator ("#").
     * @return the selector string
     * @stable ICU 49
     */
    var selector: String? = null

    /**
     * @return the selector's numeric value, or NO_NUMERIC_VALUE if !isSelectorNumeric()
     * @stable ICU 49
     */
    var selectorValue: Double = MessagePattern.NO_NUMERIC_VALUE

    /**
     * @return the nested message
     * @stable ICU 49
     */
    val message: MessageNode?
      get() = msgNode

    var numericValue = MessagePattern.NO_NUMERIC_VALUE
    var msgNode: MessageNode? = null

    override val patternString: String
      get() = msgNode?.contents?.joinToString("") { it.patternString } ?: ""
  }
}
