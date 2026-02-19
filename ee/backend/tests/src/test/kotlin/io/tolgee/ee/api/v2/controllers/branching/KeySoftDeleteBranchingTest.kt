package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.SoftDeleteBranchingTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.branching.BranchServiceImpl
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class KeySoftDeleteBranchingTest : AbstractSpringTest() {
  @Autowired
  lateinit var branchService: BranchServiceImpl

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  private lateinit var testData: SoftDeleteBranchingTestData

  @BeforeEach
  fun setup() {
    eeSubscriptionRepository.deleteAll()
    cacheManager.getCache(Caches.EE_SUBSCRIPTION)?.clear()
    testData = SoftDeleteBranchingTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `soft-delete key, create branch, hard-delete from branch, merge`() {
    // Soft-delete key1
    keyService.softDeleteMultiple(listOf(testData.key1.id))

    // Create a test branch from main
    val testBranch =
      branchService.createBranch(
        testData.project.id,
        "test",
        testData.mainBranch.id,
        testData.user,
      )

    // Hard-delete key1 (permanent delete from trash)
    keyService.hardDeleteMultiple(listOf(testData.key1.id))

    // Merge test branch into main
    val merge = dryRunMerge(testBranch)
    applyMerge(merge)

    // Assert: key1 is completely gone, merge succeeded without errors
    keyService
      .findOptional(testData.key1.id)
      .isPresent.assert
      .isFalse()
    // key2 and key3 still exist on main
    keyService.find(testData.project.id, "key2", null, "main").assert.isNotNull
    keyService.find(testData.project.id, "key3", null, "main").assert.isNotNull
  }

  @Test
  fun `soft-delete key, create branch, hard-delete from initial branch, merge`() {
    // Soft-delete key2
    keyService.softDeleteMultiple(listOf(testData.key2.id))

    // Create a test branch from main
    val testBranch =
      branchService.createBranch(
        testData.project.id,
        "test",
        testData.mainBranch.id,
        testData.user,
      )

    // Hard-delete key2 from main (permanent delete from trash)
    keyService.hardDeleteMultiple(listOf(testData.key2.id))

    // Merge test branch into main
    val merge = dryRunMerge(testBranch)
    applyMerge(merge)

    // Assert: key2 is completely gone, merge succeeded
    keyService
      .findOptional(testData.key2.id)
      .isPresent.assert
      .isFalse()
    // key1 and key3 still exist on main
    keyService.find(testData.project.id, "key1", null, "main").assert.isNotNull
    keyService.find(testData.project.id, "key3", null, "main").assert.isNotNull
  }

  @Test
  fun `soft-delete key, create new key with same name on same branch`() {
    // Soft-delete key3
    keyService.softDeleteMultiple(listOf(testData.key3.id))

    // Create new key3 on main with same name (should succeed since old one is soft-deleted)
    val newKey3 =
      keyService.create(
        testData.project,
        CreateKeyDto(
          name = "key3",
          translations = mapOf("en" to "New key 3 translation"),
          branch = "main",
        ),
      )

    // Assert: new key3 exists on main and is different from the old one
    val activeKey3 = keyService.find(testData.project.id, "key3", null, "main")
    activeKey3.assert.isNotNull
    activeKey3!!.id.assert.isEqualTo(newKey3.id)
    activeKey3.id.assert.isNotEqualTo(testData.key3.id)

    // Old key3 should still be in trash
    val trashedKeys = keyService.findSoftDeletedByIdsAndProjectId(listOf(testData.key3.id), testData.project.id)
    trashedKeys.assert.hasSize(1)
  }

  private fun dryRunMerge(testBranch: Branch): BranchMerge {
    return branchService.dryRunMerge(
      testBranch.refresh()!!,
      testData.mainBranch.refresh()!!,
    )
  }

  private fun applyMerge(merge: BranchMerge) {
    branchService.applyMerge(testData.project.id, merge.id)
  }

  private fun Branch.refresh(): Branch? {
    return branchRepository.findById(this.id).orElse(null)
  }
}
