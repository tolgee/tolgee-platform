package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.ConnectedAppsController
import io.tolgee.ee.api.v2.hateoas.model.ConnectedAppModel
import io.tolgee.ee.api.v2.hateoas.model.ConnectedAppProjectModel
import io.tolgee.ee.service.connectedApps.ConnectedAppView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class ConnectedAppModelAssembler :
  RepresentationModelAssemblerSupport<ConnectedAppView, ConnectedAppModel>(
    ConnectedAppsController::class.java,
    ConnectedAppModel::class.java,
  ) {
  override fun toModel(entity: ConnectedAppView): ConnectedAppModel {
    return ConnectedAppModel(
      id = entity.grant.id,
      clientId = entity.grant.clientId,
      clientName = entity.clientName,
      scopes = entity.grant.issuedTokenScopeValues,
      allProjects = entity.allProjects,
      projects = entity.projects.map { ConnectedAppProjectModel(it.id, it.name) },
      authorizedAt = entity.grant.createdAt?.time ?: Date().time,
      lastActiveAt = entity.grant.accessTokenIssuedAt?.time,
    )
  }
}
