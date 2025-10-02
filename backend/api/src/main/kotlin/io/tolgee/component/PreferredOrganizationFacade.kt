package io.tolgee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.model.UserPreferences
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
    val preferredOrganization = getCurrentUserPreferences()?.preferredOrganization
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

  private fun getCurrentUserPreferences(): UserPreferences? {
    val userId = authenticationFacade.authenticatedUser.id

    val inReadOnlyMode = authenticationFacade.isReadOnly
    if (inReadOnlyMode) {
      // Avoid modifying operations in read-only mode
      return userPreferencesService.find(userId)
    }

    return userPreferencesService.findOrCreate(userId)
  }
}
