package io.tolgee.ee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.fixtures.seedConnectedGrant
import io.tolgee.ee.service.connectedApps.ConnectedAppService
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.util.Date

@SpringBootTest
class ConnectedAppServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var connectedAppService: ConnectedAppService

  @Autowired
  lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `lists only consented grants with a live refresh token`() {
    val connected = seedGrant()
    val pending = seedGrant(consented = false)
    val lapsed = seedGrant(refreshExpired = true)

    val ids = connectedAppService.find(testData.user.id, Pageable.ofSize(20)).content.map { it.grant.id }

    ids.assert.contains(connected.id)
    ids.assert.doesNotContain(pending.id, lapsed.id)
  }

  @Test
  fun `hides grants of another user and of an unregistered client`() {
    val unregistered = seedGrant(clientId = "no-such-client")
    val otherUser = dbPopulator.createUserIfNotExists("connected-apps-other@tolgee.io")
    val foreign = seedGrant(userAccountId = otherUser.id)

    val ids = connectedAppService.find(testData.user.id, Pageable.ofSize(20)).content.map { it.grant.id }

    ids.assert.doesNotContain(unregistered.id, foreign.id)
  }

  @Test
  fun `revoking deletes the grant and records the user as initiator`() {
    val grant = seedGrant()

    connectedAppService.revoke(grant.id, testData.user.id)

    grantRepository.existsById(grant.id).assert.isFalse()
    val events =
      authAuditEventRepository.findAll().filter {
        it.type == AuthAuditEventType.OAUTH_GRANT_REVOKED && it.targetId == grant.id
      }
    events.assert.hasSize(1)
    events
      .first()
      .data!!["initiator"]
      .assert
      .isEqualTo("USER")
  }

  @Test
  fun `revoking a lapsed grant of the caller still works`() {
    val lapsed = seedGrant(refreshExpired = true)

    connectedAppService.revoke(lapsed.id, testData.user.id)

    grantRepository.existsById(lapsed.id).assert.isFalse()
  }

  @Test
  fun `revoking a foreign or missing grant is reported as missing`() {
    val otherUser = dbPopulator.createUserIfNotExists("connected-apps-foreign@tolgee.io")
    val foreign = seedGrant(userAccountId = otherUser.id)

    assertThatThrownBy { connectedAppService.revoke(foreign.id, testData.user.id) }
      .isInstanceOf(NotFoundException::class.java)
    assertThatThrownBy { connectedAppService.revoke(foreign.id + 999999, testData.user.id) }
      .isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `revoking all deletes every grant of the caller and leaves other users alone`() {
    val mine = seedGrant()
    val otherUser = dbPopulator.createUserIfNotExists("connected-apps-untouched@tolgee.io")
    val foreign = seedGrant(userAccountId = otherUser.id)

    connectedAppService.revokeAllForUser(testData.user.id).assert.isEqualTo(1)

    grantRepository.existsById(mine.id).assert.isFalse()
    grantRepository.existsById(foreign.id).assert.isTrue()
  }

  private fun seedGrant(
    userAccountId: Long = testData.user.id,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
    consented: Boolean = true,
    refreshExpired: Boolean = false,
  ): OAuth2Grant =
    executeInNewTransaction {
      seedConnectedGrant(
        grantRepository = grantRepository,
        user = userAccountService.get(userAccountId),
        now = currentDateProvider.date,
        clientId = clientId,
        consented = consented,
        refreshExpired = refreshExpired,
      )
    }
}
