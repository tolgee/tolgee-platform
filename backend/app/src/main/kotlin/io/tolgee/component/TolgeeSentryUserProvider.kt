package io.tolgee.component

import io.sentry.protocol.User
import io.sentry.spring.jakarta.SentryUserProvider
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.util.RequestIpProvider
import org.springframework.stereotype.Component

@Component
class TolgeeSentryUserProvider(
  private val authenticationFacade: AuthenticationFacade,
  private val requestIpProvider: RequestIpProvider,
  private val projectHolder: ProjectHolder,
) : SentryUserProvider {
  override fun provideUser(): User? {
    return authenticationFacade.authenticatedUserOrNull?.let { user ->
      return User().apply {
        name = user.username
        username = user.username
        email = user.username
        id = user.id.toString()
        ipAddress = requestIpProvider.getClientIp()
        projectHolder.projectOrNull?.let { project ->
          setData(mapOf("projectId" to project.id.toString()))
        }
      }
    }
  }
}
