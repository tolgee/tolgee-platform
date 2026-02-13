package io.tolgee.ee.api.v2.controllers.activity

import io.tolgee.ActivityTestUtil
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchRevisionData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.fixtures.satisfies
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.branching.Branch
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions

@SpringBootTest
@AutoConfigureMockMvc
class ProjectActivityBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BranchRevisionData
  private lateinit var defaultBranch: Branch

  @Autowired
  private lateinit var activityTestUtil: ActivityTestUtil

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = BranchRevisionData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
    defaultBranch = testData.defaultBranch
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filters activity list by branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthPost(
      "translations",
      mapOf("key" to "default_key", "translations" to mapOf("en" to "default text")),
    ).andIsOk
    val defaultRevisionId = waitForNewRevisionId()

    performProjectAuthPost(
      "translations",
      mapOf(
        "key" to testData.firstKey.name,
        "translations" to mapOf("en" to "updated"),
        "branch" to testData.devBranch.name,
      ),
    ).andIsOk
    val devRevisionId = waitForNewRevisionId(excluding = defaultRevisionId)

    waitForNotThrowing(timeout = 5000, pollTime = 500) {
      val devRevisions = getRevisionIds("activity?branch=${testData.devBranch.name}")
      devRevisions.assert.contains(devRevisionId)
      devRevisions.assert.doesNotContain(defaultRevisionId)
    }

    waitForNotThrowing(timeout = 5000, pollTime = 500) {
      val mainRevisions = getRevisionIds("activity?branch=${Branch.DEFAULT_BRANCH_NAME}")
      mainRevisions.assert.contains(defaultRevisionId)
      mainRevisions.assert.doesNotContain(devRevisionId)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filters modified entities by branch in detail`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    val defaultKeyId =
      performProjectAuthPost("keys", CreateKeyDto(name = "default_key"))
        .andIsCreated
        .andReturn()
        .mapResponseTo<Map<String, Any>>()["id"] as Int

    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf("keyIds" to listOf(testData.firstKey.id, defaultKeyId.toLong()), "tags" to listOf("tag_1")),
    ).andIsOk.waitForJobCompleted()

    val revisionId = activityTestUtil.getLastRevision(project.id)!!.id
    val defaultBranchId = getDefaultBranchId()

    val expectedDev = getModifiedEntities(revisionId, testData.devBranch.id)
    val expectedMain = getModifiedEntities(revisionId, defaultBranchId)

    val apiDev = getModifiedEntitiesFromApi(revisionId, testData.devBranch.name)
    val apiMain = getModifiedEntitiesFromApi(revisionId, Branch.DEFAULT_BRANCH_NAME)

    apiDev.assert.isNotEmpty
    apiMain.assert.isNotEmpty
    expectedDev.assert.containsAll(apiDev)
    expectedMain.assert.containsAll(apiMain)
  }

  private fun getRevisionIds(url: String): Set<Long> {
    val response = performProjectAuthGet(url).andIsOk.andReturn()
    val body = response.mapResponseTo<Map<String, Any?>>()
    val embedded = body["_embedded"] as? Map<*, *> ?: return emptySet()
    val activities = embedded["activities"] as? List<Map<String, Any?>> ?: return emptySet()
    return activities.mapNotNull { (it["revisionId"] as? Number)?.toLong() }.toSet()
  }

  private fun getModifiedEntities(
    revisionId: Long,
    branchId: Long,
  ): Set<String> {
    return entityManager
      .createQuery(
        """
        from ActivityModifiedEntity ame
        where ame.activityRevision.id = :revisionId
          and ame.branchId = :branchId
        """.trimIndent(),
        ActivityModifiedEntity::class.java,
      ).setParameter("revisionId", revisionId)
      .setParameter("branchId", branchId)
      .resultList
      .map { "${it.entityClass}:${it.entityId}" }
      .toSet()
  }

  private fun getModifiedEntitiesFromApi(
    revisionId: Long,
    branchName: String,
  ): Set<String> {
    val response =
      performProjectAuthGet("activity/revisions/$revisionId/modified-entities?branch=$branchName&size=50")
        .andIsOk
        .andReturn()
    val body = response.mapResponseTo<Map<String, Any?>>()
    val embedded = body["_embedded"] as? Map<*, *> ?: return emptySet()
    val modifiedEntities = embedded["modifiedEntities"] as? List<Map<String, Any?>> ?: return emptySet()
    return modifiedEntities
      .mapNotNull {
        val entityClass = it["entityClass"] as? String ?: return@mapNotNull null
        val entityId = (it["entityId"] as? Number)?.toLong() ?: return@mapNotNull null
        "$entityClass:$entityId"
      }.toSet()
  }

  private fun getDefaultBranchId(): Long {
    return defaultBranch.id
  }

  private fun ResultActions.waitForJobCompleted() =
    andAssertThatJson {
      node("id").isNumber.satisfies {
        waitFor(pollTime = 2000) {
          val job = batchJobService.findJobDto(it.toLong())
          job?.status?.completed == true
        }
      }
    }

  private fun waitForNewRevisionId(excluding: Long? = null): Long {
    var revisionId: Long? = null
    waitForNotThrowing(timeout = 5000, pollTime = 200) {
      val revision = activityTestUtil.getLastRevision(project.id) ?: throw AssertionError("No activity revision found")
      if (excluding != null && revision.id == excluding) {
        throw AssertionError("Activity revision didn't change yet")
      }
      revisionId = revision.id
    }
    return revisionId!!
  }
}
