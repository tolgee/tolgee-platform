package io.tolgee.socketio

import com.corundumstudio.socketio.HandshakeData
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.JwtTokenProvider
import io.tolgee.service.ApiKeyService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.stereotype.Component

@Component
class SocketIoProjectProvider(
  private val apiKeyService: ApiKeyService,
  private val jwtTokenProvider: JwtTokenProvider,
  private val securityService: SecurityService,
  private val projectService: ProjectService,
) {

  fun getProject(handshakeData: HandshakeData): Project {
    handshakeData.urlParams["apiKey"]?.get(0)?.let {
      val apiKey = apiKeyService.getApiKey(it).orElse(null)
      securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
      return apiKey.project
    }

    handshakeData.urlParams["jwtToken"]?.get(0)?.let { jwtTokenString ->
      handshakeData.urlParams["projectId"]?.get(0)?.let { projectIdString ->
        val jwtToken = jwtTokenProvider.resolveToken(jwtTokenString)
        jwtTokenProvider.validateToken(jwtToken)
        val userAccount = jwtTokenProvider.getUser(jwtToken)
        val projectId = projectIdString.toLong()
        securityService.checkAnyProjectPermission(projectId = projectId, userAccount)
        return projectService.get(projectId).orElseThrow { NotFoundException() }
      }
    } ?: let { throw AuthenticationException(Message.GENERAL_JWT_ERROR) }
  }
}
