package io.tolgee.service

import com.github.jknack.handlebars.Handlebars
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.providers.tolgee.LLMParams
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.PromptVariable
import io.tolgee.exceptions.NotFoundException
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service
import java.io.Writer

@Service
class PromptService(
  private val securityService: SecurityService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val translationService: TranslationService,
  private val fileStorage: FileStorage,
  private val screenshotService: ScreenshotService
) {

  fun encodeScreenshot(number: Long, type: String): String {
    return "[[screenshot_${type}_$number]]"
  }

  fun encodeScreenshots(list: List<Any>, type: String): String {
    return list
      .mapIndexed { index, _ -> encodeScreenshot(index.toLong(), type) }
      .joinToString("\n")
  }

  fun getVariables(
    projectId: Long,
    keyId: Long,
    targetLanguageId: Long,
  ): MutableList<PromptVariable> {
    securityService.checkLanguageViewPermission(
      projectId,
      listOf(targetLanguageId),
    )
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, projectId)
    val project = projectService.get(projectId) ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)

    val tLanguage =
      languageService.find(targetLanguageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val sLanguage = languageService.get(project.baseLanguage!!.id, projectId)

    val sTranslation = translationService.find(key, sLanguage)
    val tTranslation = translationService.find(key, tLanguage)


    val variables = mutableListOf<PromptVariable>()

    variables.add(PromptVariable("source", sLanguage.name))
    variables.add(PromptVariable("target", tLanguage.name))

    variables.add(PromptVariable("projectName", project.name))
    variables.add(PromptVariable("projectDescription", project.aiTranslatorPromptDescription ?: ""))
    variables.add(PromptVariable("sourceText", sTranslation.get().text ?: ""))
    variables.add(PromptVariable("targetText", tTranslation.get().text ?: ""))
    variables.add(PromptVariable("keyName", key.name))

    val screenshots = key.keyScreenshotReferences

    variables.add(
      PromptVariable(
        "allScreenshots",
        encodeScreenshots(screenshots, "full")
      )
    )

    variables.add(
      PromptVariable(
        "allSmallScreenshots",
        encodeScreenshots(screenshots, "small")
      )
    )

    variables.add(
      PromptVariable(
        "firstScreenshot",
        encodeScreenshots(screenshots.take(1), "full")
      )
    )

    variables.add(
      PromptVariable(
        "firstSmallScreenshot",
        encodeScreenshots(screenshots.take(1), "small")
      )
    )

    variables.add(
      PromptVariable(
        "fragmentReturnJson",
        """Return result in json
```
{
   "translation": <translation>,
   "description": <description>
}
```"""
      )
    )

    return variables
  }

  fun getPrompt(data: PromptTestDto): String {
    val params = getVariables(data.projectId, data.keyId, data.targetLanguageId)
    val handlebars = Handlebars()

    val mapParams = params.map { it.name to Handlebars.SafeString(it.value) }.toMap()

    val template = handlebars.compileInline(data.template)
    val prompt = template.apply(mapParams)
    return prompt
  }

  fun getLlmMessages(prompt: String, data: PromptTestDto): List<LLMParams.Companion.LlmMessage> {
    val key = keyService.find(data.keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)

    val pattern = Regex("\\[\\[screenshot_(full|small)_(\\d+)]]")

    val parts = pattern.splitWithMatches(prompt)
    return parts.map {
      if (pattern.matches(it)) {
        val match = pattern.matchEntire(it) ?: throw Error()
        // Extract size and number from the match groups
        val size = match.groups[1]!!.value // full or small
        val number = match.groups[2]!!.value.toInt() // number
        val screenshot = key.keyScreenshotReferences[number].screenshot
        val file = if (size === "full") {
          screenshot.filename
        } else {
          screenshot.middleSizedFilename ?: screenshot.filename
        }
        val image = fileStorage.readFile(screenshotService.getScreenshotPath(file))
        LLMParams.Companion.LlmMessage(
          type = LLMParams.Companion.LlmMessageType.IMAGE,
          image = image
        )
      } else {
        LLMParams.Companion.LlmMessage(
          type = LLMParams.Companion.LlmMessageType.TEXT,
          text = it
        )
      }
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

    return result
  }
}

