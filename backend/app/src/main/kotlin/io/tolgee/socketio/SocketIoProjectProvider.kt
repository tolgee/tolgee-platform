package io.tolgee.socketio

import com.corundumstudio.socketio.HandshakeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.Project
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.JwtTokenProviderImpl
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.stereotype.Component

@Component
class SocketIoProjectProvider(
  private val apiKeyService: io.tolgee.service.ApiKeyService,
  private val jwtTokenProvider: JwtTokenProviderImpl,
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
        securityService.checkAnyProjectPermission(projectId = projectId, userAccount.id)
        return projectService.get(projectId)
      }
    } ?: let { throw AuthenticationException(io.tolgee.constants.Message.GENERAL_JWT_ERROR) }
  }
}
