package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.OrganizationStatsTestData
import io.tolgee.repository.OrganizationUsageCounterRepository
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.service.organization.OrganizationUsageCounterService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OrganizationUsageCounterServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var organizationStatsService: OrganizationStatsService

  @Autowired
  lateinit var organizationUsageCounterService: OrganizationUsageCounterService

  @Autowired
  lateinit var organizationUsageCounterRepository: OrganizationUsageCounterRepository

  lateinit var testData: OrganizationStatsTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationStatsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `getCounts returns values matching the slow-query recount`() {
    // The maintenance listener seeds the counter during test data setup, so the row
    // already exists here with values reflecting the seeded entities.
    val orgId = testData.organization.id

    val counts = organizationUsageCounterService.getCounts(orgId)

    assertThat(counts.keys).isEqualTo(organizationStatsService.getKeyCount(orgId))
    assertThat(counts.translations).isEqualTo(organizationStatsService.getTranslationCount(orgId))
    assertThat(organizationUsageCounterRepository.findByOrganizationId(orgId)).isNotNull
  }

  @Test
  fun `applyDelta increments the maintained counts`() {
    val orgId = testData.organization.id
    val seeded = organizationUsageCounterService.getCounts(orgId)

    organizationUsageCounterService.applyDelta(orgId, keyDelta = 3, translationDelta = -2)

    val after = organizationUsageCounterService.getCounts(orgId)
    assertThat(after.keys).isEqualTo(seeded.keys + 3)
    assertThat(after.translations).isEqualTo(seeded.translations - 2)
  }

  @Test
  fun `forceRecompute heals a drifted counter`() {
    val orgId = testData.organization.id
    organizationUsageCounterService.getCounts(orgId) // seed
    // Drift the counter manually so it disagrees with the slow query.
    organizationUsageCounterService.applyDelta(orgId, keyDelta = 999, translationDelta = -50)

    val recomputed = organizationUsageCounterService.forceRecompute(orgId)

    assertThat(recomputed.keys).isEqualTo(organizationStatsService.getKeyCount(orgId))
    assertThat(recomputed.translations).isEqualTo(organizationStatsService.getTranslationCount(orgId))
  }

  @Test
  fun `reconcileOne heals a drifted counter and stamps last_reconciled_at`() {
    val orgId = testData.organization.id
    organizationUsageCounterService.getCounts(orgId)
    organizationUsageCounterService.applyDelta(orgId, keyDelta = 100, translationDelta = 100)

    organizationUsageCounterService.reconcileOne(orgId)

    val row = organizationUsageCounterRepository.findByOrganizationId(orgId)
    assertThat(row).isNotNull
    assertThat(row!!.keyCount).isEqualTo(organizationStatsService.getKeyCount(orgId))
    assertThat(row.translationCount).isEqualTo(organizationStatsService.getTranslationCount(orgId))
    assertThat(row.lastReconciledAt).isNotNull
  }

  @Test
  fun `getCountsWithBoundaryVerification returns counter when far from limit`() {
    val orgId = testData.organization.id
    organizationUsageCounterService.getCounts(orgId)
    // Drift the counter, but keep limits high so threshold isn't hit.
    organizationUsageCounterService.applyDelta(orgId, keyDelta = 5, translationDelta = 5)

    val counts =
      organizationUsageCounterService.getCountsWithBoundaryVerification(
        orgId,
        keyLimit = 1_000_000,
        translationLimit = 1_000_000,
      )

    // Returns the drifted counter value, no recount triggered.
    val stored = organizationUsageCounterRepository.findByOrganizationId(orgId)!!
    assertThat(counts.keys).isEqualTo(stored.keyCount)
    assertThat(counts.translations).isEqualTo(stored.translationCount)
  }

  @Test
  fun `getCountsWithBoundaryVerification recounts and heals when near limit`() {
    val orgId = testData.organization.id
    val seeded = organizationUsageCounterService.getCounts(orgId)
    // Drift the counter so the cached value exceeds the threshold for the chosen limit.
    organizationUsageCounterService.applyDelta(orgId, keyDelta = 100, translationDelta = 0)

    val drifted = organizationUsageCounterRepository.findByOrganizationId(orgId)!!.keyCount
    val limit = drifted // counter is at 100% of limit → threshold (95%) triggers verify

    val counts =
      organizationUsageCounterService.getCountsWithBoundaryVerification(
        orgId,
        keyLimit = limit,
        translationLimit = null,
      )

    // Returns the recount, not the drifted counter.
    assertThat(counts.keys).isEqualTo(seeded.keys)
    // Counter row has been healed to match the recount.
    val healed = organizationUsageCounterRepository.findByOrganizationId(orgId)!!
    assertThat(healed.keyCount).isEqualTo(seeded.keys)
  }

  @Test
  fun `getCountsWithBoundaryVerification skips verification when limit is null or non-positive`() {
    val orgId = testData.organization.id
    organizationUsageCounterService.getCounts(orgId)
    organizationUsageCounterService.applyDelta(orgId, keyDelta = 10, translationDelta = 10)
    val drifted = organizationUsageCounterRepository.findByOrganizationId(orgId)!!

    val counts =
      organizationUsageCounterService.getCountsWithBoundaryVerification(
        orgId,
        keyLimit = null,
        translationLimit = -1,
      )

    // Drift is preserved — boundary verification never fires.
    assertThat(counts.keys).isEqualTo(drifted.keyCount)
    assertThat(counts.translations).isEqualTo(drifted.translationCount)
  }
}
