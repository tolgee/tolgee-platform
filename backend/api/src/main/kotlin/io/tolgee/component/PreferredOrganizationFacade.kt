package io.tolgee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.stereotype.Component

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class PreferredOrganizationFacade(
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
    var preferred = preferences.preferredOrganization
    if (preferred == null || !organizationRoleService.canUserViewOrPublic(user, preferred.id)) {
      preferred = userPreferencesService.refreshPreferredOrganization(user.id) ?: return null
    }

    return getPrivateModel(preferred.id)
  }

  fun getPrivateModel(organizationId: Long): PrivateOrganizationModel? {
    val user = authenticationFacade.authenticatedUser
    val view = organizationService.findPrivateView(organizationId, user.id) ?: return null
    val isAtLeastMember = organizationRoleService.canUserViewAtLeastMember(user, organizationId)
    val limitedView =
      !user.isSupporterOrAdmin() && !organizationRoleService.canUserViewStrict(user.id, organizationId)
    return privateOrganizationModelAssembler.toModel(
      view,
      enabledFeaturesProvider.get(view.organization.id),
      isAtLeastMember,
      limitedView,
    )
  }
}
