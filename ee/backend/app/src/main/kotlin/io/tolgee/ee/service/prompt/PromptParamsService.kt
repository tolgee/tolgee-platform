package io.tolgee.ee.service.prompt

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.dtos.LlmParams
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.key.Key
import io.tolgee.service.key.ScreenshotService
import io.tolgee.util.ImageConverter
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Base64

@Service
class PromptParamsService(private val screenshotService: ScreenshotService, private val fileStorage: FileStorage) {
  fun getParamsFromPrompt(
    prompt: String,
    key: Key?,
    priority: LlmProviderPriority,
  ): LlmParams {
    val pattern = Regex("\\[\\[screenshot_(full|small)_(\\d+)]]")

    var preparedPrompt = prompt

    val shouldOutputJson = preparedPrompt.contains(PromptFragmentsService.LLM_MARK_JSON)
    if (shouldOutputJson) {
      preparedPrompt = preparedPrompt.replace(PromptFragmentsService.LLM_MARK_JSON, "")
    }
    val parts = pattern.splitWithMatches(preparedPrompt)
    val messages =
      parts.mapNotNull {
        if (pattern.matches(it)) {
          val match = pattern.matchEntire(it) ?: throw Error()
          // Extract size and id from the match groups
          val size = match.groups[1]!!.value // full or small
          val id = match.groups[2]!!.value.toLong() // number
          val screenshot = key?.keyScreenshotReferences?.find { it.screenshot.id == id }?.screenshot
          if (screenshot == null) {
            null
          } else {
            val image = getHighlitedScreenshot(size, screenshot, key)

            LlmParams.Companion.LlmMessage(
              type = LlmParams.Companion.LlmMessageType.IMAGE,
              image = Base64.getEncoder().encodeToString(image),
            )
          }
        } else {
          LlmParams.Companion.LlmMessage(
            type = LlmParams.Companion.LlmMessageType.TEXT,
            text = it,
          )
        }
      }
    return LlmParams(messages, shouldOutputJson, priority)
  }

  fun getHighlitedScreenshot(
    size: String,
    screenshot: io.tolgee.model.Screenshot,
    key: Key
  ): ByteArray? {
    val file =
      if (size == "full") {
        screenshot.filename
      } else {
        screenshot.middleSizedFilename ?: screenshot.filename
      }

    val filePath = screenshotService.getScreenshotPath(file)

    if (screenshot.keyScreenshotReferences.find { it.key.id == key.id } !== null) {
      val converter =
        ImageConverter(
          ByteArrayInputStream(
            fileStorage.readFile(filePath),
          ),
        )
      return converter.highlightKeys(screenshot, listOf(key.id)).toByteArray()
    } else {
      return fileStorage.readFile(filePath)
    }
  }

  // Helper function to split and keep matches
  fun Regex.splitWithMatches(input: String): List<String> {
    val result = mutableListOf<String>()
    var lastIndex = 0

    this.findAll(input).forEach { match ->
      // Add text before the match if exists
      if (match.range.first > lastIndex) {
        result.add(input.substring(lastIndex, match.range.first))
      }
      // Add the match itself
      result.add(match.value)
      lastIndex = match.range.last + 1
    }

    // Add remaining text after last match
    if (lastIndex < input.length) {
      result.add(input.substring(lastIndex))
    }

    if (result.isEmpty()) {
      result.add("")
    }

    return result
  }
}
