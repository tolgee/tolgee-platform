package io.tolgee.ee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ConnectedAppsTestData
import io.tolgee.ee.service.connectedApps.ConnectedAppService
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable

@SpringBootTest
class ConnectedAppServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var connectedAppService: ConnectedAppService

  @Autowired
  lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  lateinit var testData: ConnectedAppsTestData

  @BeforeEach
  fun setup() {
    testData = ConnectedAppsTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists only consented grants with a live refresh token`() {
    val connected = testData.addConnectedGrant()
    val pending = testData.addPendingGrant()
    val lapsed = testData.addLapsedGrant()
    testDataService.saveTestData(testData.root)

    val ids = connectedAppService.find(testData.user.id, Pageable.ofSize(20)).content.map { it.grant.id }

    ids.assert.contains(connected.id)
    ids.assert.doesNotContain(pending.id, lapsed.id)
  }

  @Test
  fun `hides grants of another user and of an unregistered client`() {
    val unregistered = testData.addConnectedGrant(clientId = "no-such-client")
    val foreign = testData.addConnectedGrant(accountBuilder = testData.otherUserAccountBuilder)
    testDataService.saveTestData(testData.root)

    val ids = connectedAppService.find(testData.user.id, Pageable.ofSize(20)).content.map { it.grant.id }

    ids.assert.doesNotContain(unregistered.id, foreign.id)
  }

  @Test
  fun `revoking deletes the grant and records the user as initiator`() {
    val grant = testData.addConnectedGrant()
    testDataService.saveTestData(testData.root)

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
    val lapsed = testData.addLapsedGrant()
    testDataService.saveTestData(testData.root)

    connectedAppService.revoke(lapsed.id, testData.user.id)

    grantRepository.existsById(lapsed.id).assert.isFalse()
  }

  @Test
  fun `revoking a foreign or missing grant is reported as missing`() {
    val foreign = testData.addConnectedGrant(accountBuilder = testData.otherUserAccountBuilder)
    testDataService.saveTestData(testData.root)

    assertThatThrownBy { connectedAppService.revoke(foreign.id, testData.user.id) }
      .isInstanceOf(NotFoundException::class.java)
    assertThatThrownBy { connectedAppService.revoke(foreign.id + 999999, testData.user.id) }
      .isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `revoking all deletes every grant of the caller and leaves other users alone`() {
    val mine = testData.addConnectedGrant()
    val foreign = testData.addConnectedGrant(accountBuilder = testData.otherUserAccountBuilder)
    testDataService.saveTestData(testData.root)

    connectedAppService.revokeAllForUser(testData.user.id).assert.isEqualTo(1)

    grantRepository.existsById(mine.id).assert.isFalse()
    grantRepository.existsById(foreign.id).assert.isTrue()
  }

  @Test
  fun `revoking all sweeps a pending consent without auditing it`() {
    val connected = testData.addConnectedGrant()
    val pending = testData.addPendingGrant()
    testDataService.saveTestData(testData.root)

    connectedAppService.revokeAllForUser(testData.user.id).assert.isEqualTo(2)

    grantRepository.existsById(pending.id).assert.isFalse()
    val pendingEvents =
      authAuditEventRepository.findAll().filter {
        it.type == AuthAuditEventType.OAUTH_GRANT_REVOKED && it.targetId == pending.id
      }
    pendingEvents.assert.isEmpty()
    val connectedEvents =
      authAuditEventRepository.findAll().filter {
        it.type == AuthAuditEventType.OAUTH_GRANT_REVOKED && it.targetId == connected.id
      }
    connectedEvents.assert.hasSize(1)
  }
}
