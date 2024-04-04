package io.tolgee.formats.apple.`in`.strings

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.apple.`in`.guessLanguageFromPath
import io.tolgee.formats.apple.`in`.guessNamespaceFromPath
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class StringsFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  private var state = State.OUTSIDE
  private var key: String? = null
  private var value: String? = null
  private var wasLastCharEscape = false
  private var currentComment: StringBuilder? = null
  private var lastChar: Char? = null

  override fun process() {
    parseFileToContext()
    context.namespace = guessNamespaceFromPath(context.file.name)
  }

  private fun parseFileToContext() {
    context.file.data.decodeToString().forEachIndexed { index, char ->
      if (!wasLastCharEscape && char == '\\') {
        wasLastCharEscape = true
        return@forEachIndexed
      }

      when (state) {
        State.OUTSIDE -> {
          if (char == '\"') {
            state = State.INSIDE_KEY
            key = ""
          }

          if (char == '=') {
            if (key == null) {
              throw ImportCannotParseFileException(context.file.name, "Unexpected '=' character on position $index")
            }

            state = State.EXPECT_VALUE
          }

          if (char == '/' && lastChar == '/') {
            currentComment = null
            state = State.INSIDE_INLINE_COMMENT
          }

          if (lastChar == '/' && char == '*') {
            currentComment = null
            state = State.INSIDE_BLOCK_COMMENT
          }
        }

        State.EXPECT_VALUE -> {
          if (char == '\"') {
            state = State.INSIDE_VALUE
            value = ""
          }
        }

        State.INSIDE_KEY -> {
          when {
            char == '\"' && !wasLastCharEscape -> state = State.OUTSIDE
            wasLastCharEscape && char == 'n' -> key += "\n"
            wasLastCharEscape && char == 'r' -> key += "\r"
            else -> key += char
          }
        }

        State.INSIDE_VALUE -> {
          when {
            char == '\"' && !wasLastCharEscape -> {
              state = State.OUTSIDE
              onPairParsed()
              key = null
              value = null
            }

            wasLastCharEscape && char == 'n' -> value += "\n"
            wasLastCharEscape && char == 'r' -> value += "\r"

            else -> value += char
          }
        }

        State.INSIDE_INLINE_COMMENT -> {
          // inline comment is ignored
          if (char == '\n' && !wasLastCharEscape) {
            state = State.OUTSIDE
          }
        }

        State.INSIDE_BLOCK_COMMENT -> {
          when {
            lastChar == '*' && char == '/' && !wasLastCharEscape -> {
              currentComment?.let {
                it.deleteCharAt(it.length - 1)
              }
              state = State.OUTSIDE
            }

            wasLastCharEscape && char == 'n' -> addToCurrentComment("\n")
            wasLastCharEscape && char == 'r' -> addToCurrentComment("\r")

            else -> addToCurrentComment(char.toString())
          }
        }
      }
      lastChar = char
      wasLastCharEscape = false
    }
  }

  private fun addToCurrentComment(char: CharSequence) {
    currentComment = (currentComment ?: StringBuilder()).also { it.append(char) }
  }

  private fun onPairParsed() {
    val converted =
      messageConvertor.convert(
        rawData = value,
        languageTag = languageName,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addKeyDescription(key ?: return, currentComment?.toString())
    context.addTranslation(
      key ?: return,
      languageName,
      converted.message,
      rawData = value,
      convertedBy = importFormat,
      pluralArgName = converted.pluralArgName,
    )
    currentComment = null
  }

  private val languageName: String by lazy {
    guessLanguageFromPath(context.file.name)
  }

  companion object {
    private val importFormat = ImportFormat.STRINGS

    private val messageConvertor = importFormat.messageConvertor
  }

  enum class State {
    OUTSIDE,
    INSIDE_INLINE_COMMENT,
    INSIDE_BLOCK_COMMENT,
    INSIDE_KEY,
    INSIDE_VALUE,
    EXPECT_VALUE,
  }
}
