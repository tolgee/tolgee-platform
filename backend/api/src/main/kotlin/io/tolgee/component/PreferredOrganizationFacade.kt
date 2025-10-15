package io.tolgee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.security.authentication.AuthenticationFacade
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
) {
  fun getPreferred(): PrivateOrganizationModel? {
    val preferences = userPreferencesService.findOrCreate(authenticationFacade.authenticatedUser.id)
    val preferredOrganization = preferences.preferredOrganization
    if (preferredOrganization != null) {
      val view =
        organizationService.findPrivateView(preferredOrganization.id, authenticationFacade.authenticatedUser.id)
          ?: return null
      return this.privateOrganizationModelAssembler.toModel(
        view,
        enabledFeaturesProvider.get(view.organization.id),
      )
    }
    return null
  }
}
