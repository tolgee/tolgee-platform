package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.ee.api.v2.hateoas.assemblers.ConnectedAppModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.ConnectedAppModel
import io.tolgee.ee.service.connectedApps.ConnectedAppService
import io.tolgee.ee.service.connectedApps.ConnectedAppView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/user/connected-apps")
@Tag(name = "Connected apps")
class ConnectedAppsController(
  private val connectedAppService: ConnectedAppService,
  private val connectedAppModelAssembler: ConnectedAppModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<ConnectedAppView>,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  /**
   * Unlike the session listing this is not behind super authentication: it discloses a client name,
   * a scope set and a project binding, all of which the user picked on a consent screen, and none of
   * the location history that gate exists to protect.
   */
  @GetMapping(value = [""])
  @Operation(summary = "Get apps connected to the current user's account")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun getAll(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ConnectedAppModel> {
    val apps = connectedAppService.find(authenticationFacade.authenticatedUser.id, pageable)
    return pagedResourcesAssembler.toModel(apps, connectedAppModelAssembler)
  }

  @DeleteMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Disconnect an app")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun revoke(
    @PathVariable id: Long,
  ) {
    connectedAppService.revoke(id, authenticationFacade.authenticatedUser.id)
  }
}
