package io.tolgee.hateoas.auth

import io.tolgee.api.v2.controllers.InitialDataController
import io.tolgee.security.authentication.TolgeeAuthentication
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class AuthInfoModelAssembler :
  RepresentationModelAssemblerSupport<TolgeeAuthentication, AuthInfoModel>(
    InitialDataController::class.java,
    AuthInfoModel::class.java,
  ) {
  override fun toModel(entity: TolgeeAuthentication): AuthInfoModel {
    return AuthInfoModel(
      isReadOnly = entity.isReadOnly,
    )
  }
}
