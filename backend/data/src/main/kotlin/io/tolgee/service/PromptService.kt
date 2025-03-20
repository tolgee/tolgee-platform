package io.tolgee.service

import com.github.jknack.handlebars.Handlebars
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.PromptVariable
import io.tolgee.exceptions.NotFoundException
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestParam
import software.amazon.awssdk.services.translate.model.InvalidRequestException

@Service
class PromptService(
  private val securityService: SecurityService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val translationService: TranslationService
) {
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

    val tLanguage = languageService.find(targetLanguageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val sLanguage = languageService.get(project.baseLanguage!!.id, projectId)

    val sTranslation = translationService.find(key, sLanguage)
    val tTranslation = translationService.find(key, tLanguage)


    val variables = mutableListOf<PromptVariable>()

    variables.add(PromptVariable("target", sLanguage.name))
    variables.add(PromptVariable("source", tLanguage.name))

    variables.add(PromptVariable("projectName", project.name))
    variables.add(PromptVariable("projectDescription", project.aiTranslatorPromptDescription ?: ""))
    variables.add(PromptVariable("sourceText", sTranslation.get().text ?: ""))
    variables.add(PromptVariable("targetText", tTranslation.get().text ?: ""))
    variables.add(PromptVariable("keyName", key.name))

    return variables
  }

  fun getPrompt(data: PromptTestDto): String {
    val params = getVariables(data.projectId, data.keyId, data.targetLanguageId)
    val handlebars = Handlebars()

    val mapParams = params.map { it.name to it.value }.toMap()

    val template = handlebars.compileInline(data.template)
    val prompt = template.apply(mapParams)
    return prompt
  }
}
