package io.tolgee.ee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ConnectedAppsTestData
import io.tolgee.ee.service.connectedApps.ConnectedAppService
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * `ConnectedAppService.find` and `.revokeAllForUser` both guard on an empty
 * `OAuth2ClientRegistry.clients` before ever calling `findConnected` with an empty `clientIds`
 * collection, which JPQL's `in ()` cannot execute. Isolated in its own class so the mocked
 * registry does not affect `ConnectedAppServiceTest`'s other cases.
 */
@SpringBootTest
class ConnectedAppServiceEmptyRegistryTest : AbstractSpringTest() {
  @Autowired
  lateinit var connectedAppService: ConnectedAppService

  @Autowired
  lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  @MockitoBean
  lateinit var clientRegistry: OAuth2ClientRegistry

  lateinit var testData: ConnectedAppsTestData

  @BeforeEach
  fun setup() {
    testData = ConnectedAppsTestData()
    whenever(clientRegistry.clients).thenReturn(emptyList())
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `find returns an empty page when no client is registered`() {
    testData.addConnectedGrant()
    testDataService.saveTestData(testData.root)

    val page = connectedAppService.find(testData.user.id, Pageable.ofSize(20))

    page.content.assert.isEmpty()
    page.totalElements.assert.isEqualTo(0)
  }

  @Test
  fun `revokeAllForUser still deletes when no client is registered`() {
    val grant = testData.addConnectedGrant()
    testDataService.saveTestData(testData.root)

    connectedAppService.revokeAllForUser(testData.user.id).assert.isEqualTo(1)

    grantRepository.existsById(grant.id).assert.isFalse()
  }
}
