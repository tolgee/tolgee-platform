package io.tolgee.service.mcp

import io.tolgee.component.BackendUrlProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service

data class IssuedUploadUrl(
  val uploadUrl: String,
  val expiresInSeconds: Long,
)

@Service
class McpImageUploadUrlService(
  private val jwtService: JwtService,
  private val userAccountService: UserAccountService,
  private val backendUrlProvider: BackendUrlProvider,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun issueUploadUrl(userAccountId: Long): IssuedUploadUrl {
    val lifetimeMs = tolgeeProperties.mcp.imageUploadUrlExpirationMs
    val token =
      jwtService.emitTicket(
        userAccountId,
        JwtService.TicketType.IMG_UPLOAD,
        expiresAfter = lifetimeMs,
      )
    return IssuedUploadUrl(
      uploadUrl = "${backendUrlProvider.url}/v2/public/image-upload?token=$token",
      expiresInSeconds = lifetimeMs / 1000,
    )
  }

  fun resolveUserFromUploadToken(token: String): UserAccount {
    val auth = jwtService.validateTicket(token, JwtService.TicketType.IMG_UPLOAD)
    return userAccountService.get(auth.userAccount.id)
  }
}
