package io.tolgee.hateoas.organization.apps

import io.tolgee.model.Organization
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AppAvailableOrganizationModelAssembler :
  RepresentationModelAssembler<Organization, AppAvailableOrganizationModel> {
  override fun toModel(entity: Organization): AppAvailableOrganizationModel {
    return AppAvailableOrganizationModel(
      id = entity.id,
      name = entity.name,
      slug = entity.slug,
    )
  }
}
