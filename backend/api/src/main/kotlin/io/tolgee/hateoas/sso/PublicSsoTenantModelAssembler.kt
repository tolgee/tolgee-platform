package io.tolgee.hateoas.sso

import io.tolgee.api.ISsoTenant
import io.tolgee.api.v2.controllers.V2UserController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PublicSsoTenantModelAssembler :
  RepresentationModelAssemblerSupport<ISsoTenant, PublicSsoTenantModel>(
    V2UserController::class.java,
    PublicSsoTenantModel::class.java,
  ) {
  override fun toModel(tenant: ISsoTenant): PublicSsoTenantModel {
    return PublicSsoTenantModel(
      domain = tenant.domain,
      global = tenant.global,
      force = tenant.force,
    )
  }
}
