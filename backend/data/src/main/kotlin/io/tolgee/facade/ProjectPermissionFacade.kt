package io.tolgee.facade

import io.tolgee.constants.Message
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.dtos.request.project.RequestWithLanguagePermissions
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Language
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.language.LanguageService
import org.springframework.stereotype.Component

@Component
class ProjectPermissionFacade(
  private val authenticationFacade: AuthenticationFacade,
  private val languageService: LanguageService,
) {
  fun checkNotCurrentUser(userId: Long) {
    if (userId == authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(Message.CANNOT_SET_YOUR_OWN_PERMISSIONS)
    }
  }

  fun getLanguagesAndCheckFromProject(
    languages: Set<Long>?,
    projectId: Long,
  ): Set<Language> {
    languages?.let {
      val languageEntities = languageService.findByIdIn(languages)
      languageEntities.forEach {
        if (it.project.id != projectId) {
          throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
        }
      }
      return languageEntities.toSet()
    }
    return setOf()
  }

  fun getLanguages(
    params: RequestWithLanguagePermissions,
    projectId: Long,
  ): LanguagePermissions {
    return LanguagePermissions(
      view = this.getLanguagesAndCheckFromProject(params.viewLanguages, projectId),
      translate =
        this.getLanguagesAndCheckFromProject(
          params.translateLanguages ?: params.languages,
          projectId,
        ),
      stateChange =
        this.getLanguagesAndCheckFromProject(
          params.stateChangeLanguages,
          projectId,
        ),
      suggest =
        this.getLanguagesAndCheckFromProject(
          params.suggestLanguages,
          projectId,
        ),
    )
  }
}
