package io.tolgee.websocket

import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.ScopedCredential
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.authentication.withSecurityContext
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/** Who may subscribe to which websocket topic. See docs/websocket/README.md. */
@Component
class WebsocketSubscribeAuthorizer(
  @Lazy
  private val securityService: SecurityService,
) : Logging {
  fun decide(
    authentication: TolgeeAuthentication?,
    destination: String?,
  ): SubscribeDecision {
    try {
      return decideForDestination(authentication, destination)
    } catch (e: NotFoundException) {
      logger.debug("Denying websocket subscription to {}: project not found", destination, e)
      return SubscribeDecision.DENY
    }
  }

  enum class SubscribeDecision {
    ALLOW,
    DENY,
    UNAUTHENTICATED,
  }

  private fun decideForDestination(
    authentication: TolgeeAuthentication?,
    destination: String?,
  ): SubscribeDecision {
    val projectId = knownTopicId(PROJECT_TOPIC, destination)
    if (projectId != null) {
      val auth = authentication ?: return SubscribeDecision.UNAUTHENTICATED
      if (isProjectSubscribeAllowed(auth, projectId)) return SubscribeDecision.ALLOW
      return denied(destination)
    }

    val userId = knownTopicId(USER_TOPIC, destination)
    if (userId != null) {
      val auth = authentication ?: return SubscribeDecision.UNAUTHENTICATED
      if (isUserSubscribeAllowed(auth, userId)) return SubscribeDecision.ALLOW
      return denied(destination)
    }

    logger.debug("Denying websocket subscription to unrecognized destination {}", destination)
    return SubscribeDecision.DENY
  }

  private fun knownTopicId(
    topic: Regex,
    destination: String?,
  ): Long? {
    val match = topic.find(destination.orEmpty()) ?: return null
    if (WebsocketEventType.entries.none { it.typeName == match.groups["type"]!!.value }) return null
    return match.groups["id"]!!.value.toLongOrNull()
  }

  private fun denied(destination: String?): SubscribeDecision {
    logger.debug("Denying websocket subscription to {}", destination)
    return SubscribeDecision.DENY
  }

  private fun isProjectSubscribeAllowed(
    authentication: TolgeeAuthentication,
    projectId: Long,
  ): Boolean {
    try {
      withSecurityContext(authentication) {
        securityService.checkProjectPermission(
          projectId = projectId,
          requiredPermission = Scope.KEYS_VIEW,
          user = authentication.principal,
        )
      }
    } catch (e: PermissionException) {
      logger.debug("User / API key does not have required scopes", e)
      return false
    }
    return true
  }

  private fun isUserSubscribeAllowed(
    authentication: TolgeeAuthentication,
    userId: Long,
  ): Boolean {
    if (authentication.credentials is ScopedCredential) {
      return false
    }
    return authentication.principal.id == userId
  }

  private companion object {
    val PROJECT_TOPIC = "^/projects/(?<id>[0-9]+)/(?<type>[^/]+)$".toRegex()
    val USER_TOPIC = "^/users/(?<id>[0-9]+)/(?<type>[^/]+)$".toRegex()
  }
}
