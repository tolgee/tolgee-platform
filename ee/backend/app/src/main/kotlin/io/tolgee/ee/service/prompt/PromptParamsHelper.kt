package io.tolgee.ee.service.prompt

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.service.prompt.PromptVariablesHelper.Companion.ScreenshotSize
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.key.Key
import io.tolgee.service.key.ScreenshotService
import io.tolgee.util.ScreenshotKeysHighlighter
import io.tolgee.util.regexSplitAndMatch
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.util.Base64

@Component
class PromptParamsHelper(
  private val screenshotService: ScreenshotService,
  private val fileStorage: FileStorage,
) {
  fun getParamsFromPrompt(
    prompt: String,
    key: Key?,
    priority: LlmProviderPriority,
  ): LlmParams {
    val pattern = Regex("\\[\\[screenshot_(${ScreenshotSize.FULL.value}|${ScreenshotSize.SMALL.value})_(\\d+)]]")

    var preparedPrompt = prompt

    val screenshotReferences = key?.let { screenshotService.getAllKeyScreenshotReferences(key) }

    val shouldOutputJson = preparedPrompt.contains(PromptFragmentsHelper.LLM_MARK_JSON)
    if (shouldOutputJson) {
      preparedPrompt = preparedPrompt.replace(PromptFragmentsHelper.LLM_MARK_JSON, "")
    }
    val parts = regexSplitAndMatch(pattern, preparedPrompt)
    val messages =
      parts.mapNotNull {
        if (pattern.matches(it)) {
          val match = pattern.matchEntire(it) ?: throw Error()
          // Extract size and id from the match groups
          val size = match.groups[1]!!.value // full or small
          val id = match.groups[2]!!.value.toLong() // number
          val screenshot = screenshotReferences?.find { it.screenshot.id == id }?.screenshot
          if (screenshot == null) {
            null
          } else {
            val image = getHighlightedScreenshot(size, screenshot, key)

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

  fun getHighlightedScreenshot(
    size: String,
    screenshot: io.tolgee.model.Screenshot,
    key: Key,
  ): ByteArray? {
    val file =
      if (size == "full") {
        screenshot.filename
      } else {
        screenshot.middleSizedFilename ?: screenshot.filename
      }

    val filePath = screenshotService.getScreenshotPath(file)

    if (screenshot.keyScreenshotReferences.find { it.key.id == key.id } !== null) {
      val highlighter =
        ScreenshotKeysHighlighter(
          ByteArrayInputStream(
            fileStorage.readFile(filePath),
          ),
        )
      return highlighter.highlightKeys(screenshot, listOf(key.id)).toByteArray()
    } else {
      return fileStorage.readFile(filePath)
    }
  }
}
