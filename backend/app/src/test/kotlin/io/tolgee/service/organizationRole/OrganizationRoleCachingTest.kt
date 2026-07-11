/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.organizationRole

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.cacheable.UserOrganizationRoleDto
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.model.OrganizationRole
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class OrganizationRoleCachingTest : AbstractSpringTest() {
  @Suppress("LateinitVarOverridesLateinitVar")
  @MockitoSpyBean
  @Autowired
  override lateinit var organizationRoleRepository: OrganizationRoleRepository

  private lateinit var testData: OrganizationTestData

  @MockitoSpyBean
  @Autowired
  private lateinit var authenticationFacade: AuthenticationFacade

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    clearCaches()
    Mockito.reset(organizationRoleRepository)
  }

  @Test
  fun `it caches organization roles`() {
    organizationRoleService.getDto(testData.pepaOrg.id, testData.pepa.id)
    assertRepositoryInvocationOnce()
    assertCachePopulated(testData.pepaOrg.id, testData.pepa.id)
    organizationRoleService.getDto(testData.pepaOrg.id, testData.pepa.id)
    assertRepositoryInvocationOnce()
  }

  @Test
  fun `it evicts cache on grant role`() {
    populateCache(testData.pepaOrg.id, testData.kvetoslav.id)
    organizationRoleService.grantRoleToUser(testData.kvetoslav, testData.pepaOrg, OrganizationRoleType.MEMBER)
    assertCacheEvicted(testData.kvetoslav.id, testData.pepa.id)
  }

  @Test
  fun `it evicts on leave`() {
    doAnswer { UserAccountDto.fromEntity(testData.pepa) }.whenever(authenticationFacade).authenticatedUser
    populateCache(testData.pepaOrg.id, testData.pepa.id)
    organizationRoleService.leave(testData.pepaOrg.id)
    assertCacheEvicted(testData.pepaOrg.id, testData.pepa.id)
  }

  @Test
  fun `it evicts on remove user`() {
    populateCache(testData.pepaOrg.id, testData.pepa.id)
    organizationRoleService.removeUser(testData.pepa.id, testData.pepaOrg.id)
    assertCacheEvicted(testData.pepaOrg.id, testData.pepa.id)
  }

  @Test
  fun `it evicts on set member role`() {
    populateCache(testData.pepaOrg.id, testData.pepa.id)
    organizationRoleService.setMemberRole(
      organizationId = testData.pepaOrg.id,
      userId = testData.pepa.id,
      dto = SetOrganizationRoleDto(OrganizationRoleType.MEMBER),
    )
    assertCacheEvicted(testData.pepaOrg.id, testData.pepa.id)
  }

  @Test
  fun `it evicts on invitation accept`() {
    populateCache(testData.jirinaOrg.id, testData.pepa.id)
    organizationRoleService.acceptInvitation(
      organizationRole =
        OrganizationRole(
          type = OrganizationRoleType.MEMBER,
        ).apply {
          organization = testData.jirinaOrg
          user = testData.pepa
        },
      userAccount = testData.pepa,
    )
    assertCacheEvicted(testData.jirinaOrg.id, testData.pepa.id)
  }

  private fun assertRepositoryInvocationOnce() {
    verify(organizationRoleRepository, times(1)).findOneByUserIdAndOrganizationId(any(), any())
  }

  fun populateCache(
    organizationId: Long,
    userId: Long,
  ) {
    organizationRoleService.getDto(organizationId, userId)
    assertCachePopulated(organizationId, userId)
  }

  private fun assertCachePopulated(
    organizationId: Long,
    userId: Long,
  ) {
    getCacheItem(organizationId, userId).assert.isNotNull
  }

  private fun getCacheItem(
    organizationId: Long,
    userId: Long,
  ): UserOrganizationRoleDto? =
    cacheManager
      .getCache(Caches.ORGANIZATION_ROLES)!!
      .get(arrayListOf(organizationId, userId))
      ?.get() as UserOrganizationRoleDto?

  private fun assertCacheEvicted(
    organizationId: Long,
    userId: Long,
  ) {
    getCacheItem(organizationId, userId).assert.isNull()
  }
}
