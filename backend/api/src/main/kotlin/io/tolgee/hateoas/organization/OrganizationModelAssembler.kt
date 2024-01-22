package io.tolgee.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.views.OrganizationView
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
    val link = linkTo<OrganizationController> { get(view.organization.slug) }.withSelfRel()
    val basePermission = view.organization.basePermission
    return OrganizationModel(
      view.organization.id,
      view.organization.name,
      view.organization.slug,
      view.organization.description,
      basePermissions = permissionModelAssembler.toModel(basePermission),
      currentUserRole = view.currentUserRole,
      avatar = avatarService.getAvatarLinks(view.organization.avatarHash),
    ).add(link)
  }
}
