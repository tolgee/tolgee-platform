package io.tolgee.facade

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.stereotype.Component

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class PrivateOrganizationModelFacade(
  private val authenticationFacade: AuthenticationFacade,
  private val userPreferencesService: UserPreferencesService,
  private val privateOrganizationModelAssembler: PrivateOrganizationModelAssembler,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
) {
  fun getPreferred(): PrivateOrganizationModel? {
    val user = authenticationFacade.authenticatedUser
    val preferences = userPreferencesService.findOrCreate(user.id)
    val preferred = preferences.preferredOrganization ?: return null
    val accessible =
      preferred.takeIf { organizationRoleService.canUserViewOrPublic(user, it.id) }
        ?: userPreferencesService.refreshPreferredOrganization(user.id)
        ?: return null

    return getPrivateModelWithoutAuthorization(accessible.id)
  }

  fun getPrivateModelWithoutAuthorization(organizationId: Long): PrivateOrganizationModel? {
    val user = authenticationFacade.authenticatedUser
    val view = organizationService.findPrivateView(organizationId, user.id) ?: return null
    val isAtLeastMember = organizationRoleService.hasAnyOrganizationRole(user.id, organizationId)
    val limitedView = !organizationRoleService.canUserView(user, organizationId)
    return privateOrganizationModelAssembler.toModel(
      view,
      enabledFeaturesProvider.get(view.organization.id),
      isAtLeastMember,
      limitedView,
    )
  }
}
