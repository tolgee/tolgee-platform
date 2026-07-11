package io.tolgee.ee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.enums.Scope
import io.tolgee.model.translationAgency.TranslationAgency
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.PermissionService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EePermissionService(
  private val permissionService: PermissionService,
  private val organizationService: OrganizationService,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun setUserDirectPermission(
    projectId: Long,
    userId: Long,
    scopes: Set<Scope>,
    languages: LanguagePermissions,
  ): Permission {
    validateLanguagePermissions(
      languagePermissions = languages,
      scopes = scopes,
    )

    val permission = permissionService.getOrCreateDirectPermission(projectId, userId)

    permission.type = null
    permission.scopes = scopes.toTypedArray()

    permissionService.setPermissionLanguages(permission, languages, projectId)

    return permissionService.save(permission)
  }

  fun setOrganizationBasePermission(
    organizationId: Long,
    scopes: Set<Scope>,
  ) {
    val permission = organizationService.get(organizationId).basePermission
    permission.scopes = scopes.toTypedArray()
    permission.type = null
    permissionService.save(permission)
  }

  fun createForInvitation(
    invitation: Invitation,
    params: CreateProjectInvitationParams,
  ): Permission {
    val scopes = Scope.parse(params.scopes)

    validateLanguagePermissions(params.languagePermissions, scopes)

    val permission =
      Permission(
        type = null,
        invitation = invitation,
        project = params.project,
        scopes = scopes.toTypedArray(),
        agency =
          params.agencyId?.let {
            entityManager.getReference(TranslationAgency::class.java, it)
          },
      )

    permissionService.setPermissionLanguages(permission, params.languagePermissions, params.project.id)

    return permissionService.save(permission)
  }

  private fun validateLanguagePermissions(
    languagePermissions: LanguagePermissions,
    scopes: Set<Scope>?,
  ) {
    if (scopes.isNullOrEmpty()) {
      throw BadRequestException(Message.SCOPES_HAS_TO_BE_SET)
    }
    val hasTranslateLanguages = !languagePermissions.translate.isNullOrEmpty()
    val hasViewLanguages = !languagePermissions.view.isNullOrEmpty()
    val hasStateChangeLanguages = !languagePermissions.stateChange.isNullOrEmpty()

    if ((hasViewLanguages || hasTranslateLanguages || hasStateChangeLanguages) && scopes.contains(Scope.ADMIN)) {
      throw BadRequestException(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
    }

    val scopesExpanded = Scope.expand(scopes)
    val hasView = scopesExpanded.contains(Scope.TRANSLATIONS_VIEW)
    val hasEdit = scopesExpanded.contains(Scope.TRANSLATIONS_EDIT)
    val hasStateEdit = scopesExpanded.contains(Scope.TRANSLATIONS_STATE_EDIT)

    if (hasViewLanguages && !hasView) {
      throw BadRequestException(Message.CANNOT_SET_VIEW_LANGUAGES_WITHOUT_TRANSLATIONS_VIEW_SCOPE)
    }

    if (hasTranslateLanguages && !hasEdit) {
      throw BadRequestException(Message.CANNOT_SET_TRANSLATE_LANGUAGES_WITHOUT_TRANSLATIONS_EDIT_SCOPE)
    }

    if (hasStateChangeLanguages && !hasStateEdit) {
      throw BadRequestException(Message.CANNOT_SET_STATE_CHANGE_LANGUAGES_WITHOUT_TRANSLATIONS_STATE_EDIT_SCOPE)
    }
  }
}
