package io.tolgee.hateoas.permission

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PermissionModelAssembler :
  RepresentationModelAssemblerSupport<IPermission, PermissionModel>(
    V2UserController::class.java,
    PermissionModel::class.java,
  ) {
  override fun toModel(entity: IPermission): PermissionModel {
    return PermissionModel(
      scopes = Scope.expand(entity.scopes),
      permittedLanguageIds = entity.translateLanguageIds,
      viewLanguageIds = entity.viewLanguageIds,
      translateLanguageIds = entity.translateLanguageIds,
      stateChangeLanguageIds = entity.stateChangeLanguageIds,
      suggestLanguageIds = entity.suggestLanguageIds,
      type = entity.type,
    )
  }
}
