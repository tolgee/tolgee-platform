package io.tolgee.security.oauth2

import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OAuth2GrantAuditTest : AbstractOAuth2FlowTest() {
  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  @Test
  fun `completing an authorization records an authorization event`() {
    completeFlow(testData.project.id)

    val events =
      authAuditEventRepository.findAll().filter {
        it.type == AuthAuditEventType.OAUTH_GRANT_AUTHORIZED
      }
    events.assert.hasSize(1)
    events
      .first()
      .userAccountId.assert
      .isEqualTo(testData.user.id)
    events
      .first()
      .data!!["clientId"]
      .assert
      .isEqualTo(CLIENT_ID)
  }

  @Test
  fun `a client revoking its own token records the client as initiator`() {
    val token = accessToken(testData.project.id)

    driver.revoke(token, CLIENT_ID).andIsOk

    val events =
      authAuditEventRepository.findAll().filter {
        it.type == AuthAuditEventType.OAUTH_GRANT_REVOKED
      }
    events.assert.hasSize(1)
    events
      .first()
      .data!!["initiator"]
      .assert
      .isEqualTo("CLIENT")
  }
}
