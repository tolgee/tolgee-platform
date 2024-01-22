package io.tolgee.hateoas.permission

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.ComputedPermissionDto
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ComputedPermissionModelAssembler(
  private val permissionModelAssembler: PermissionModelAssembler,
) : RepresentationModelAssemblerSupport<ComputedPermissionDto, ComputedPermissionModel>(
    V2UserController::class.java,
    ComputedPermissionModel::class.java,
  ) {
  override fun toModel(dto: ComputedPermissionDto): ComputedPermissionModel {
    return ComputedPermissionModel(
      permissionModelAssembler.toModel(dto),
      dto.origin,
    )
  }
}
