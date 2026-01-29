package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.OrganizationStatsTestData
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OrganizationStatsServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var organizationStatsService: OrganizationStatsService

  lateinit var testData: OrganizationStatsTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationStatsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `getProjectKeyCount counts unique keys across branches`() {
    // First project has:
    // - key1 (main only) = 1 unique key
    // - key2 (main and feature) = 1 unique key
    // - key3 (feature only) = 1 unique key
    // - key4 with namespace1 (main and feature) = 1 unique key
    // - key4 with namespace2 (main only) = 1 unique key
    // - key4 without namespace (main only) = 1 unique key
    // Total: 6 unique keys
    val projectKeyCount = organizationStatsService.getProjectKeyCount(testData.project.id)
    assertThat(projectKeyCount).isEqualTo(6)
  }

  @Test
  fun `getProjectKeyCount counts unique keys in second project`() {
    // Second project has:
    // - key1 (main and feature) = 1 unique key
    // - key5 (main only) = 1 unique key
    // Total: 2 unique keys
    val projectKeyCount = organizationStatsService.getProjectKeyCount(testData.secondProject.id)
    assertThat(projectKeyCount).isEqualTo(2)
  }

  @Test
  fun `getKeyCount counts unique keys across all projects in organization`() {
    // Organization total:
    // First project: 6 unique keys
    // Second project: 2 unique keys
    // Total: 8 unique keys
    val orgKeyCount = organizationStatsService.getKeyCount(testData.organization.id)
    assertThat(orgKeyCount).isEqualTo(8)
  }

  @Test
  fun `getTranslationCount counts unique translations across branches`() {
    // First project translations (unique key+language combinations with non-empty text):
    // - key1 (main): EN = 1
    // - key2 (main and feature, counted once): EN, DE = 2
    // - key3 (feature): EN = 1
    // - key4/namespace1 (main and feature, counted once): DE = 1
    // - key4/namespace2: no translations = 0
    // - key4/no namespace: empty translation = 0
    // First project total: 5
    //
    // Second project translations:
    // - key1 (main and feature, counted once): EN = 1
    // - key5 (main): EN, DE = 2
    // Second project total: 3
    //
    // Organization total: 8
    val translationCount = organizationStatsService.getTranslationCount(testData.organization.id)
    assertThat(translationCount).isEqualTo(8)
  }
}
