package io.tolgee.component

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.model.views.OrganizationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.UserPreferencesService
import org.springframework.stereotype.Component

@Component
class PreferredOrganizationFacade(
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val userPreferencesService: UserPreferencesService,
  private val organizationModelAssembler: OrganizationModelAssembler
) {

  fun getPreferred(): OrganizationModel? {
    val preferences = userPreferencesService.findOrCreate(authenticationFacade.userAccount.id)
    val preferredOrganization = preferences.preferredOrganization
    if (preferredOrganization != null) {
      val roleType = organizationRoleService.findType(preferredOrganization.id)
      val view = OrganizationView.of(preferredOrganization, roleType)
      return this.organizationModelAssembler.toModel(view)
    }
    return null
  }
}
