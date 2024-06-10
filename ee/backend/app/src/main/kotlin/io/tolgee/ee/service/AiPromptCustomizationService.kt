package io.tolgee.ee.service

import io.tolgee.ee.data.SetLanguagePromptCustomizationRequest
import io.tolgee.ee.data.SetProjectPromptCustomizationRequest
import io.tolgee.model.Project
import io.tolgee.service.language.LanguageService
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
  ): Project {
    val project = projectService.get(projectId)
    project.aiTranslatorPromptDescription = request.description
    return projectService.save(project)
  }

  @Transactional
  fun setLanguagePromptCustomization(
    projectId: Long,
    languageId: Long,
    dto: SetLanguagePromptCustomizationRequest,
  ) {
    val language = languageService.getEntity(languageId, projectId)
    language.aiTranslatorPromptDescription = dto.description
    languageService.save(language)
  }
}
