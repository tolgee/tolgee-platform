package io.tolgee.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.Organization
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class SimpleOrganizationModelAssembler(
  private val avatarService: AvatarService,
  private val permissionModelAssembler: PermissionModelAssembler,
) : RepresentationModelAssemblerSupport<Organization, SimpleOrganizationModel>(
    OrganizationController::class.java,
    SimpleOrganizationModel::class.java,
  ) {
  override fun toModel(entity: Organization): SimpleOrganizationModel {
    val link = linkTo<OrganizationController> { get(entity.slug ?: "") }.withSelfRel()
    return SimpleOrganizationModel(
      entity.id,
      entity.name,
      entity.slug,
      entity.description,
      permissionModelAssembler.toModel(entity.basePermission),
      avatarService.getAvatarLinks(entity.avatarHash),
    ).add(link)
  }
}
