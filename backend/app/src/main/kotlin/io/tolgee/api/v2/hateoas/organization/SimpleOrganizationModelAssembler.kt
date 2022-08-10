package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.model.Organization
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class SimpleOrganizationModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<Organization, SimpleOrganizationModel>(
  OrganizationController::class.java, SimpleOrganizationModel::class.java
) {
  override fun toModel(entity: Organization): SimpleOrganizationModel {
    val link = linkTo<OrganizationController> { get(entity.slug ?: "") }.withSelfRel()
    return SimpleOrganizationModel(
      entity.id,
      entity.name!!,
      entity.slug!!,
      entity.description,
      entity.basePermissions,
      avatarService.getAvatarLinks(entity.avatarHash)
    ).add(link)
  }
}
