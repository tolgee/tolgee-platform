package io.tolgee.api.v2.hateoas.permission

import io.tolgee.model.Permission
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PermissionModelAssembler() : RepresentationModelAssemblerSupport<Permission, PermissionModel>(
  null, PermissionModel::class.java
) {
  override fun toModel(entity: Permission): PermissionModel {
    return PermissionModel(entity.scopes, entity.languages.map { it.id })
  }
}
