package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.UserSessionsController
import io.tolgee.ee.api.v2.hateoas.model.UserSessionModel
import io.tolgee.model.UserSession
import io.tolgee.security.authentication.AuthenticationFacade
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class UserSessionModelAssembler(
  private val authenticationFacade: AuthenticationFacade,
) : RepresentationModelAssemblerSupport<UserSession, UserSessionModel>(
    UserSessionsController::class.java,
    UserSessionModel::class.java,
  ) {
  override fun toModel(entity: UserSession): UserSessionModel {
    return UserSessionModel(
      id = entity.id,
      type = entity.type,
      ip = entity.ip,
      userAgent = entity.userAgent,
      countryCode = entity.countryCode,
      country = entity.country,
      city = entity.city,
      createdAt = entity.createdAt?.time ?: Date().time,
      lastUsedAt = entity.lastUsedAt?.time,
      expiresAt = entity.expiresAt.time,
      isCurrent = entity.deviceId == authenticationFacade.deviceIdOrNull,
    )
  }
}
