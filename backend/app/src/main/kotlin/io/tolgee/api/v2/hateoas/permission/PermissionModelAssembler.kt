package io.tolgee.api.v2.hateoas.permission

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.model.Permission
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PermissionModelAssembler() : RepresentationModelAssemblerSupport<Permission, PermissionModel>(
  V2UserController::class.java, PermissionModel::class.java
) {
  override fun toModel(entity: Permission): PermissionModel {
    return PermissionModel(entity.scopes, entity.languages.map { it.id })
  }
}
