package io.tolgee.cache

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.annotation.DirtiesContext

@ContextRecreatingTest
@SpringBootTest(properties = ["tolgee.cache.enabled=true"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrganizationCacheEvictionTest : AbstractSpringTest() {
  private lateinit var testData: BaseTestData

  private val organization get() = testData.project.organizationOwner

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `saving an organization refreshes its cached data`() {
    cachedOrganizations()

    executeInNewTransaction {
      organizationService.save(organizationService.get(organization.id).apply { name = "Renamed" })
    }

    assertThat(cachedOrganizations().map { it?.name }).containsOnly("Renamed")
  }

  @Test
  fun `setting and removing an avatar refreshes the cached organization`() {
    cachedOrganizations()

    executeInNewTransaction {
      organizationService.setAvatar(organizationService.get(organization.id), avatar.inputStream)
    }
    assertThat(cachedOrganizations().map { it?.avatarHash }).doesNotContainNull()

    executeInNewTransaction { organizationService.removeAvatar(organizationService.get(organization.id)) }
    assertThat(cachedOrganizations().map { it?.avatarHash }).containsOnlyNulls()
  }

  private fun cachedOrganizations() =
    listOf(organizationService.findDto(organization.id), organizationService.findDto(organization.slug))

  private val avatar get() = ClassPathResource("e2eTestResources/avatars/png_avatar.png", javaClass.classLoader)
}
