package io.tolgee.formats.po.`in`

import io.tolgee.exceptions.PoParserException
import io.tolgee.formats.po.`in`.data.PoParsedTranslation
import io.tolgee.formats.po.`in`.data.PoParserMeta
import io.tolgee.formats.po.`in`.data.PoParserResult
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import java.util.Locale

class PoParser(
  private val context: FileProcessorContext,
) {
  private var expectMsgId = false
  private var expectMsgStr = false
  private var expectMsgIdPlural = false
  private var expectMsgStrPlural: Int? = null
  private var expectTranslatorComments = false
  private var expectExtractedComments = false
  private var expectReference = false
  private var expectFlags = false
  private var expectContext = false

  private var currentTranslation: PoParsedTranslation? = null
  private var valueProvided = false
  private var expectValue = false
  private var translations = mutableListOf<PoParsedTranslation>()
  private var currentSequence = StringBuilder()
  private var currentSequenceStart: Int = 1
  private var quoted = false
  private var hashed = false
  private var wasHash = false
  private var wasMsgStr = false

  private var currentEscaped = false
  private var currentLine = 1
  private var currentPosition = 1
  private var nextEscaped = false
  private var expectNewlineAfterCarriageReturn = false

  operator fun invoke(): PoParserResult {
    processInputStream()

    return PoParserResult(
      meta = processHeader(),
      translations,
    )
  }

  private fun processHeader(): PoParserMeta {
    val result = PoParserMeta()
    translations.filter { it.msgid.toString() == "" }.forEach {
      it.msgstr.split("\n").map { metaLine ->
        val trimmed = metaLine.trim()
        if (trimmed.isBlank()) {
          return@map
        }
        val colonPosition = trimmed.indexOf(":")
        val key = trimmed.substring(0 until colonPosition)
        val value = trimmed.substring(colonPosition + 1).trim()
        when (key.lowercase(Locale.getDefault())) {
          "project-id-version" -> result.projectIdVersion = value
          "language" -> result.language = value
          "plural-forms" -> result.pluralForms = value
          else -> result.other[key] = value
        }
      }
    }
    return result
  }

  private fun Char.handle() {
    when (this) {
      '\\' -> handleEscapeChar()
      '"' -> handleQuote()
      '\n' -> handleNewLine()
      '\r' -> handleCarriageReturn()
      ' ' -> handleSpace()
      '#' -> handleHash()
      else -> this.handleOther()
    }
    if (this != '#') {
      wasHash = false
    }
    currentEscaped = nextEscaped
    nextEscaped = false
    currentPosition++
  }

  private fun handleCarriageReturn() {
    expectNewlineAfterCarriageReturn = true
  }

  private fun processInputStream() {
    context.file.data.decodeToString().forEach {
      it.handle()
    }
    endTranslation()
  }

  private fun Char.handleHash() {
    if (!quoted) {
      possibleEndTranslationBefore()
      hashed = true
      wasHash = true
      return
    }
    currentSequence.append(this)
  }

  private fun possibleEndTranslationBefore() {
    if (wasMsgStr) {
      wasMsgStr = false
      endTranslation()
    }
  }

  private fun handleEscapeChar() {
    if (!currentEscaped) {
      nextEscaped = true
      return
    }
    currentSequence.append('\\')
  }

  private fun Char.handleOther() {
    if (currentEscaped) {
      val specialEscape: Char? =
        if (quoted) {
          when (this) {
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            '"' -> '"'
            '\\' -> '\\'
            else -> null
          }
        } else {
          null
        }

      if (specialEscape != null) {
        currentSequence.append(specialEscape)
      } else {
        currentSequence.append('\\')
        currentSequence.append(this)
      }
      return
    }

    if (expectNewlineAfterCarriageReturn) {
      expectNewlineAfterCarriageReturn = false
      '\r'.handleOther()
    }

    if (wasHash) {
      handleAfterHash()
      return
    }
    currentSequence.append(this)
  }

  private fun Char.handleAfterHash() {
    when {
      this == ' ' -> expectTranslatorComments = true
      this == '.' -> expectExtractedComments = true
      this == ':' -> expectReference = true
      this == ',' -> expectFlags = true
      this == '|' -> expectContext = true
    }
  }

  private fun Char.handleSpace() {
    if (hashed) {
      handleOther()
      return
    }
    if (quoted) {
      currentSequence.append(this)
      return
    }
    currentSequenceEnd()
  }

  private fun Char.handleQuote() {
    if (hashed) {
      handleOther()
      return
    }
    if (!currentEscaped) {
      if (!expectValue) {
        throwUnexpected()
      }
      if (quoted) {
        currentSequenceEnd()
      }
      quoted = !quoted
      return
    }
    currentSequence.append(this)
  }

  private fun currentSequenceEnd() {
    if (hashed) {
      handleHashedEnd()
    } else {
      if (!quoted) {
        currentSequence.handleKeyword()
      }
      if (quoted) {
        storeCurrent()
        valueProvided = true
      }
    }
    currentSequence = StringBuilder()
  }

  private fun handleHashedEnd() {
    when {
      expectTranslatorComments -> createdTranslation.meta.translatorComments.add(currentSequence.trim().toString())
      expectExtractedComments -> createdTranslation.meta.extractedComments.add(currentSequence.trim().toString())
      expectReference -> createdTranslation.meta.references.addAll(currentSequence.trim().split(" "))
      expectFlags -> createdTranslation.meta.flags.addAll(currentSequence.trim().split(" "))
      expectContext -> createdTranslation.meta.context.add(currentSequence.trim().toString())
    }
    hashed = false
    resetValueExpectations()
  }

  private fun StringBuilder.handleKeyword() {
    if (expectValue && !valueProvided) {
      throw PoParserException("Unexpected keyword '$this'", currentLine, currentSequenceStart)
    }
    resetValueExpectations()

    val current = currentSequence.toString()
    expectValue = true
    valueProvided = false
    when {
      isKeyword("msgid") -> {
        possibleEndTranslationBefore()
        expectMsgId = true
      }

      isKeyword("msgid_plural") -> {
        expectMsgIdPlural = true
      }

      isKeyword("msgstr") -> {
        expectMsgStr = true
        wasMsgStr = true
      }

      isKeyword("msgctxt") -> {
        context.fileEntity.addIssue(
          FileIssueType.PO_MSGCTXT_NOT_SUPPORTED,
          mapOf(FileIssueParamType.LINE to currentLine.toString()),
        )
      }

      current.matches("^msgstr\\[\\d+]$".toRegex()) -> {
        wasMsgStr = true
        expectMsgStrPlural = current.replace("^msgstr\\[(\\d+)]$".toRegex(), "$1").toInt()
      }

      else -> throw PoParserException("Unknown keyword '$this'", currentLine, currentSequenceStart)
    }
  }

  private fun StringBuilder.isKeyword(keyword: String): Boolean {
    return toString() == keyword
  }

  private fun handleNewLine() {
    if (hashed) {
      currentSequenceEnd()
    }
    expectNewlineAfterCarriageReturn = false
    currentPosition = 0
    currentLine++
  }

  private fun Char.throwUnexpected() {
    throw PoParserException("Unexpected character '$this'", currentLine, currentPosition)
  }

  private val createdTranslation: PoParsedTranslation
    get() {
      val ct = currentTranslation ?: PoParsedTranslation()
      currentTranslation = ct
      return ct
    }

  private fun storeCurrent() {
    createdTranslation.apply {
      if (expectMsgId) {
        this.msgid.append(currentSequence)
      }

      if (expectMsgStr) {
        this.msgstr.append(currentSequence)
      }

      if (expectMsgIdPlural) {
        this.msgidPlural.append(currentSequence)
      }

      if (expectMsgStrPlural != null) {
        addToPlurals(expectMsgStrPlural!!, currentSequence.toString())
      }
    }
  }

  private fun endTranslation() {
    storeCurrent()
    currentTranslation?.let {
      translations.add(it)
    }
    currentTranslation = null
  }

  private fun resetValueExpectations() {
    expectValue = false
    expectMsgId = false
    expectMsgStr = false
    expectMsgIdPlural = false
    expectMsgStrPlural = null
    expectValue = false
    valueProvided = false
    expectTranslatorComments = false
    expectExtractedComments = false
    expectReference = false
    expectFlags = false
    expectContext = false
  }
}
