package io.tolgee.ee.service

import io.tolgee.ee.data.SetLanguagePromptCustomizationRequest
import io.tolgee.ee.data.SetProjectPromptCustomizationRequest
import io.tolgee.service.LanguageService
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiPromptCustomizationService(
  private val projectService: ProjectService,
  private val languageService: LanguageService,
) {
  @Transactional
  fun setProjectPromptCustomization(
    projectId: Long,
    request: SetProjectPromptCustomizationRequest,
  ) {
    val project = projectService.get(projectId)
    project.aiTranslatorPromptDescription = request.description
    projectService.save(project)
  }

  @Transactional
  fun setLanguagePromptCustomization(
    projectId: Long,
    languageId: Long,
    dto: SetLanguagePromptCustomizationRequest,
  ) {
    val language = languageService.get(projectId, languageId)
    language.aiTranslatorPromptDescription = dto.description
    languageService.save(language)
  }
}
