package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.model.views.OrganizationView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<OrganizationView, OrganizationModel>(
  OrganizationController::class.java, OrganizationModel::class.java
) {
  override fun toModel(view: OrganizationView): OrganizationModel {
    val link = linkTo<OrganizationController> { get(view.slug) }.withSelfRel()
    return OrganizationModel(
      view.id,
      view.name,
      view.slug,
      view.description,
      PermissionModel(view.basePermission.scopes, view.basePermission.languages.map { it.id }),
      view.currentUserRole,
      avatarService.getAvatarLinks(view.avatarHash)
    ).add(link)
  }
}
