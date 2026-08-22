package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.repository.PermissionRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * An app install's principal is a real account so the writes an install makes have an author, but it
 * is never a member: it holds no organization role and no project permission, so it is neither
 * billed as a seat nor able to act beyond the install's granted scopes.
 */
class AppPrincipalSeatTest : AbstractSpringTest() {
  @Autowired
  private lateinit var appInstallService: AppInstallService

  @Autowired
  private lateinit var permissionRepository: PermissionRepository

  @MockitoBean
  @Autowired
  private lateinit var appManifestHttpClient: AppManifestHttpClient

  private lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registering and installing an app adds no billed seat`() {
    val orgId = testData.organization.id
    val before = seatUserIds(orgId)

    registerAndInstall()

    val after = seatUserIds(orgId)
    // The principal is a fresh account, yet the seat set is unchanged: it is filtered out by
    // membership-based counting because it holds no organization role and no project permission.
    after.assert.isEqualTo(before)
  }

  @Test
  fun `the install principal holds no organization role and no project permission`() {
    val principalId = registerAndInstall()

    executeInNewTransaction(platformTransactionManager) {
      organizationRoleRepository
        .findOneByUserIdAndOrganizationId(principalId, testData.organization.id)
        .assert
        .isNull()
      permissionRepository
        .findAllByOrganizationAndUserId(testData.organization.id, principalId)
        .assert
        .isEmpty()
    }
  }

  private fun registerAndInstall(): Long {
    return executeInNewTransaction(platformTransactionManager) {
      appInstallService
        .register(
          organization = testData.organization,
          manifestUrl = AppsTestFixtures.MANIFEST_URL,
          manifestHash = null,
          install = true,
        ).install!!
        .principal.id
    }
  }

  private fun seatUserIds(organizationId: Long): Set<Long> {
    return executeInNewTransaction(platformTransactionManager) {
      organizationRepository.getAllUserIdsInOrganizationToCountSeats(organizationId)
    }
  }
}
