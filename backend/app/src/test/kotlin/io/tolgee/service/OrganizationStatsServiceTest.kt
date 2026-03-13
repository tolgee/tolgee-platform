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
    // First project (branching enabled): 6 unique keys
    // Second project (branching enabled): 2 unique keys
    // Third project (branching disabled): nb-key1 + nb-key3 = 2 keys (nb-key2 on orphan branch excluded)
    // Total: 10 unique keys
    val orgKeyCount = organizationStatsService.getKeyCount(testData.organization.id)
    assertThat(orgKeyCount).isEqualTo(10)
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
    // Third project translations (branching disabled):
    // - nb-key1: EN = 1
    // - nb-key2 on orphan branch: excluded (branching disabled)
    // Third project total: 1
    //
    // Organization total: 9
    val translationCount = organizationStatsService.getTranslationCount(testData.organization.id)
    assertThat(translationCount).isEqualTo(9)
  }

  @Test
  fun `getKeyCount excludes branch keys when project has branching disabled`() {
    // The no-branching project has useBranching=false with:
    // - nb-key1 (no branch) = counted
    // - nb-key2 (orphan feature branch) = NOT counted (branching disabled)
    // - nb-key3 (no branch) = counted
    // This is verified via the org-wide count which includes all three projects:
    // First project: 6 + Second project: 2 + Third project: 2 = 10
    val orgKeyCount = organizationStatsService.getKeyCount(testData.organization.id)
    assertThat(orgKeyCount).isEqualTo(10)
  }

  @Test
  fun `getTranslationCount excludes branch translations when project has branching disabled`() {
    // The no-branching project has useBranching=false with:
    // - nb-key1 EN translation = counted
    // - nb-key2 EN translation on orphan branch = NOT counted (branching disabled)
    // This is verified via the org-wide count:
    // First project: 5 + Second project: 3 + Third project: 1 = 9
    val translationCount = organizationStatsService.getTranslationCount(testData.organization.id)
    assertThat(translationCount).isEqualTo(9)
  }
}
