/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.oauth2.OAuth2AuthorizationQueryService
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Lets a signed-in user see the OAuth apps they have authorized and disconnect any of them. JWT (web) auth only — this
 * is account management, not something an API key should reach.
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/user/connected-apps")
@Tag(name = "Connected apps")
class ConnectedAppsController(
  private val authenticationFacade: AuthenticationFacade,
  private val queryService: OAuth2AuthorizationQueryService,
  private val registeredClientRepository: RegisteredClientRepository,
  private val oAuth2AuthorizationConsentService: OAuth2AuthorizationConsentService,
) : IController {
  @GetMapping("")
  @Operation(summary = "List OAuth apps the current user has authorized")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun list(): List<ConnectedAppModel> {
    val principalName = authenticationFacade.authenticatedUser.id.toString()
    return queryService.findAuthorizedClients(principalName).mapNotNull { row ->
      val client = registeredClientRepository.findById(row.registeredClientId) ?: return@mapNotNull null
      val consent = oAuth2AuthorizationConsentService.findById(row.registeredClientId, principalName)
      ConnectedAppModel(
        clientId = client.clientId,
        clientName = client.clientName,
        scopes = consent?.scopes?.toList() ?: emptyList(),
        lastAuthorizedAt = row.lastAuthorizedAt?.toEpochMilli(),
      )
    }
  }

  @DeleteMapping("/{clientId}")
  @Operation(summary = "Disconnect an OAuth app (revokes all its grants for the current user)")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun revoke(
    @PathVariable clientId: String,
  ) {
    val client = registeredClientRepository.findByClientId(clientId) ?: throw NotFoundException()
    queryService.revoke(client.id, authenticationFacade.authenticatedUser.id.toString())
  }
}
