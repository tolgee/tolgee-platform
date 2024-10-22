package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.SsoProviderController
import io.tolgee.ee.data.SsoTenantDto
import io.tolgee.hateoas.ee.SsoTenantModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SsoTenantAssembler :
  RepresentationModelAssemblerSupport<SsoTenantDto, SsoTenantModel>(
    SsoProviderController::class.java,
    SsoTenantModel::class.java,
  ) {
  override fun toModel(entity: SsoTenantDto): SsoTenantModel =
    SsoTenantModel(
      authorizationUri = entity.authorizationUri,
      clientId = entity.clientId,
      clientSecret = entity.clientSecret,
      redirectUri = entity.redirectUri,
      tokenUri = entity.tokenUri,
      isEnabled = entity.isEnabled,
      jwkSetUri = entity.jwkSetUri,
      domainName = entity.domainName,
    )
}
