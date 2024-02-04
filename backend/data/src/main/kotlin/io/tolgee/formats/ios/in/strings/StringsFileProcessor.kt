package io.tolgee.formats.ios.`in`.strings

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.convertMessage
import io.tolgee.formats.ios.`in`.IOsToIcuParamConvertor
import io.tolgee.formats.ios.`in`.guessLanguageFromPath
import io.tolgee.formats.ios.`in`.guessNamespaceFromPath
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

class StringsFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  private var state = State.OUTSIDE
  private var key: String? = null
  private var value: String? = null
  private var wasLastCharEscape = false

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
        }

        State.EXPECT_VALUE -> {
          if (char == '\"') {
            state = State.INSIDE_VALUE
            value = ""
          }
        }

        State.INSIDE_KEY -> {
          if (char == '\"' && !wasLastCharEscape) {
            state = State.OUTSIDE
          } else {
            key += char
          }
        }

        State.INSIDE_VALUE -> {
          if (char == '\"' && !wasLastCharEscape) {
            state = State.OUTSIDE
            onPairParsed(key!!, value!!)
            key = null
            value = null
          } else {
            value += char
          }
        }
      }
      wasLastCharEscape = false
    }
  }

  private fun onPairParsed(
    key: String,
    value: String,
  ) {
    val convertedMessage =
      convertMessage(value, false) {
        IOsToIcuParamConvertor()
      }
    context.addTranslation(key, languageName, convertedMessage)
  }

  private val languageName: String by lazy {
    guessLanguageFromPath(context.file.name)
  }

  enum class State {
    OUTSIDE,
    INSIDE_KEY,
    INSIDE_VALUE,
    EXPECT_VALUE,
  }
}
