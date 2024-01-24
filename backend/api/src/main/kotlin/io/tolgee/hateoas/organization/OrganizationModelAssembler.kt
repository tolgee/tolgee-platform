package io.tolgee.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.dtos.queryResults.organization.OrganizationView
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationModelAssembler(
  private val avatarService: AvatarService,
  private val permissionModelAssembler: PermissionModelAssembler,
) : RepresentationModelAssemblerSupport<OrganizationView, OrganizationModel>(
    OrganizationController::class.java,
    OrganizationModel::class.java,
  ) {
  override fun toModel(view: OrganizationView): OrganizationModel {
    val link = linkTo<OrganizationController> { get(view.slug) }.withSelfRel()
    return OrganizationModel(
      view.id,
      view.name,
      view.slug,
      view.description,
      basePermissions = permissionModelAssembler.toModel(view.basePermission),
      currentUserRole = view.currentUserRole,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
    ).add(link)
  }
}
