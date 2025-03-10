package io.tolgee.service

import com.github.jknack.handlebars.Handlebars
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service

@Service
class PromptService(
  private val securityService: SecurityService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val translationService: TranslationService
) {
  fun getPrompt(data: PromptTestDto): String {
    securityService.checkLanguageViewPermission(
      data.projectId,
      listOf(data.targetLanguageId),
    )
    val key = keyService.find(data.keyId!!) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, data.projectId)
    val project = projectService.get(data.projectId) ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)

    val tLanguage = languageService.find(data.targetLanguageId, data.projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val sLanguage = languageService.get(project.baseLanguage!!.id, data.projectId)

    val sTranslation = translationService.find(key, sLanguage)
    val tTranslation = translationService.find(key, tLanguage)



    val handlebars = Handlebars()
    val params = HashMap<String, Any>()

    params.set("target", sLanguage.name)
    params.set("source", tLanguage.name)
    params.set("projectName", project.name)
    params.set("projectDescription", project.aiTranslatorPromptDescription ?: "")
    params.set("sourceText", sTranslation.get().text ?: "")
    params.set("targetText", tTranslation.get().text ?: "")

    val template = handlebars.compileInline(data.template)
    val prompt = template.apply(params)
    return prompt
  }
}
