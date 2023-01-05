package io.tolgee.api.v2.hateoas.permission

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.cacheable.IPermission
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PermissionModelAssembler() : RepresentationModelAssemblerSupport<IPermission, PermissionModel>(
  V2UserController::class.java, PermissionModel::class.java
) {
  override fun toModel(entity: IPermission): PermissionModel {
    return PermissionModel(
      scopes = entity.scopes,
      permittedLanguageIds = entity.translateLanguageIds,
      translateLanguageIds = entity.translateLanguageIds,
      stateChangeLanguageIds = entity.stateChangeLanguageIds,
      viewLanguageIds = entity.viewLanguageIds,
      type = entity.type,
      granular = entity.granular ?: false
    )
  }
}
