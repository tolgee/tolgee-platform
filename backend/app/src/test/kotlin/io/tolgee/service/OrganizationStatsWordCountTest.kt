package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.CountAllWordsOnInstanceTestData
import io.tolgee.development.testDataBuilder.data.OrganizationStatsWordCountTestData
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OrganizationStatsWordCountTest : AbstractSpringTest() {
  @Autowired
  lateinit var organizationStatsService: OrganizationStatsService

  lateinit var testData: OrganizationStatsWordCountTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationStatsWordCountTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `getWordCount sums word counts across all languages for a single key`() {
    val wordCount = organizationStatsService.getWordCount(testData.multiLangOrg.id)
    assertThat(wordCount).isEqualTo(5)
  }

  @Test
  fun `getWordCount takes MAX word count per branch and language, then sums the languages`() {
    // en: max(1, 2) = 2, de: max(4, 2) = 4 — collapsed per language, not down to one language.
    val wordCount = organizationStatsService.getWordCount(testData.branchDedupOrg.id)
    assertThat(wordCount).isEqualTo(6)
  }

  @Test
  fun `getWordCount excludes non-default branch keys when use_branching is false`() {
    val wordCount = organizationStatsService.getWordCount(testData.noBranchingOrg.id)
    assertThat(wordCount).isEqualTo(2)
  }

  @Test
  fun `getWordCount excludes empty translations`() {
    val wordCount = organizationStatsService.getWordCount(testData.emptyTranslationOrg.id)
    assertThat(wordCount).isEqualTo(0)
  }

  @Test
  fun `countAllWordsOnInstance sums branch-deduped word counts across the whole instance`() {
    val baseline = organizationStatsService.countAllWordsOnInstance()

    val cawData = CountAllWordsOnInstanceTestData()
    testDataService.saveTestData(cawData.root)

    try {
      val wordCount = organizationStatsService.countAllWordsOnInstance()
      assertThat(wordCount - baseline).isEqualTo(5)
    } finally {
      testDataService.cleanTestData(cawData.root)
    }
  }

  @Test
  fun `getWordCount collapses a name shared by a branch and no branch at all`() {
    val wordCount = organizationStatsService.getWordCount(testData.nullBranchDedupOrg.id)

    assertThat(wordCount).isEqualTo(3)
  }

  @Test
  fun `getTranslationAndWordCount agrees with the single-figure queries`() {
    listOf(
      testData.multiLangOrg,
      testData.branchDedupOrg,
      testData.noBranchingOrg,
      testData.emptyTranslationOrg,
      testData.nullBranchDedupOrg,
    ).forEach { organization ->
      val both = organizationStatsService.getTranslationAndWordCount(organization.id)
      assertThat(both.words).isEqualTo(organizationStatsService.getWordCount(organization.id))
      assertThat(both.translations).isEqualTo(organizationStatsService.getTranslationCount(organization.id))
    }
  }
}
