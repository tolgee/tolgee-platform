package io.tolgee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.model.views.OrganizationView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.QuickStartService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.stereotype.Component

@Component
class PreferredOrganizationFacade(
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val userPreferencesService: UserPreferencesService,
  private val privateOrganizationModelAssembler: PrivateOrganizationModelAssembler,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val quickStartService: QuickStartService
) {

  fun getPreferred(): PrivateOrganizationModel? {
    val preferences = userPreferencesService.findOrCreate(authenticationFacade.authenticatedUser.id)
    val preferredOrganization = preferences.preferredOrganization
    if (preferredOrganization != null) {
      val roleType = organizationRoleService.findType(preferredOrganization.id)
      val view = OrganizationView.of(preferredOrganization, roleType)
      return this.privateOrganizationModelAssembler.toModel(
        view,
        enabledFeaturesProvider.get(view.organization.id),
        quickStartService.find(authenticationFacade.authenticatedUser.id, view.organization.id)
      )
    }
    return null
  }
}
