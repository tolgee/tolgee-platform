package io.tolgee.hateoas.organization.apps

import io.tolgee.model.Organization
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class InstallingOrganizationModelAssembler : RepresentationModelAssembler<Organization, InstallingOrganizationModel> {
  override fun toModel(entity: Organization): InstallingOrganizationModel {
    return InstallingOrganizationModel(
      id = entity.id,
      name = entity.name,
      slug = entity.slug,
    )
  }
}
