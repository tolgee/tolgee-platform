package io.tolgee.activity.groups

import com.posthog.java.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityGroup
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Pageable
import java.time.Duration

class ActivityGroupsCreationTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

  @MockBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var activityGroupService: ActivityGroupService

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
  }

  lateinit var key: Key

  private fun prepareTestData() {
    testData = BaseTestData()
    testData.user.name = "Franta"
    testData.projectBuilder.apply {
      addKey {
        name = "key"
        this@ActivityGroupsCreationTest.key = this
      }
    }
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it creates the groups in time windows`() {
    prepareTestData()
    assertItGroupsInTimeWindow()
    assertItDoesNotGroupOutOfTimeWindow()
    assertItStopsGroupingDueToAge()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it creates correct groups per complex update`() {
    prepareTestData()
    performProjectAuthPut(
      "keys/${key.id}/complex-update",
      ComplexEditKeyDto(
        name = "new name",
        description = "Changed!",
        tags = listOf("tag1", "tag2"),
        translations = mapOf(testData.englishLanguage.tag to "Test"),
      ),
    ).andIsOk

    assertGroupsForActivity(
      ActivityGroupType.KEY_NAME_EDIT,
      ActivityGroupType.KEY_TAGS_EDIT,
      ActivityGroupType.SET_TRANSLATIONS,
    )

    val groups =
      activityGroupService.getProjectActivityGroups(
        projectId = testData.project.id,
        pageable = Pageable.ofSize(10),
      )

    groups
  }

  private fun assertItStopsGroupingDueToAge() {
    currentDateProvider.move(Duration.ofHours(3))

    (1..23).forEach {
      executeTranslationUpdate("Test $it")
      currentDateProvider.move(Duration.ofHours(1))
    }

    assertGroupRevisionsCount(23)

    currentDateProvider.move(Duration.ofHours(2))
    executeTranslationUpdate("Test final")
    assertGroupRevisionsCount(1)
  }

  private fun assertGroupsForActivity(vararg types: ActivityGroupType) {
    val activityRevision = findLastActivityRevision()
    val groups = getActivityGroupsForRevision(activityRevision)
    groups.map { it.type }.assert.containsExactlyInAnyOrder(*types)
  }

  private fun assertItDoesNotGroupOutOfTimeWindow() {
    // it's not the same group anymore
    currentDateProvider.move(Duration.ofMinutes(123))

    executeTranslationUpdate("Test 4")
    assertGroupRevisionsCount(1)
    assertLastGroupType(ActivityGroupType.SET_TRANSLATIONS)
    assertLastValue("Test 4")
  }

  private fun assertItGroupsInTimeWindow() {
    executeTranslationUpdate("Test")
    executeTranslationUpdate("Test 2")

    currentDateProvider.move(Duration.ofMinutes(65))
    // still the same group
    executeTranslationUpdate("Test 3")

    asserGroupCount(1)
    assertGroupRevisionsCount(3)
    assertLastGroupType(ActivityGroupType.SET_TRANSLATIONS)
    assertLastValue("Test 3")
  }

  private fun assertGroupRevisionsCount(count: Int) {
    executeInNewTransaction {
      val activityRevision = findLastActivityRevision()
      val groups = getActivityGroupsForRevision(activityRevision)
      val group = groups.single()
      entityManager.createQuery(
        """
        select count(ar) from ActivityRevision ar
        join ar.activityGroups ag
        where ag.id = :groupId
      """,
      )
        .setParameter("groupId", group.id)
        .singleResult
        .assert.isEqualTo(count.toLong())
    }
  }

  private fun asserGroupCount(count: Int) {
    executeInNewTransaction {
      val activityRevision = findLastActivityRevision()
      val groups = getActivityGroupsForRevision(activityRevision)
      groups.size.assert.isEqualTo(count)
    }
  }

  private fun assertLastGroupType(activityGroupType: ActivityGroupType) {
    executeInNewTransaction {
      val activityRevision = findLastActivityRevision()
      val groups = getActivityGroupsForRevision(activityRevision)
      groups.single().type.assert.isEqualTo(activityGroupType)
    }
  }

  private fun assertLastValue(value: String) {
    executeInNewTransaction {
      val activityRevision = findLastActivityRevision()
      val groups = getActivityGroupsForRevision(activityRevision)
      val group = groups.single()
      val modifiedEntities = getModifiedEntitiesForGroup(group)
      modifiedEntities.filter { it.entityClass == Translation::class.simpleName }
        .last()
        .modifications["text"]!!
        .new.assert.isEqualTo(value)
    }
  }

  private fun executeTranslationUpdate(value: String) {
    performProjectAuthPut(
      "translations",
      mapOf("key" to "key", "translations" to mapOf(testData.englishLanguage.tag to value)),
    ).andIsOk
  }

  private fun findLastActivityRevision(): ActivityRevision {
    return entityManager.createQuery(
      """
      select ar from ActivityRevision ar
      order by ar.timestamp desc
      limit 1
    """,
      ActivityRevision::class.java,
    ).singleResult
  }

  private fun getActivityGroupsForRevision(activityRevision: ActivityRevision): MutableList<ActivityGroup> {
    return entityManager.createQuery(
      """
      select ag from ActivityGroup ag
      join fetch ag.activityRevisions ar
      where ar.id = :activityRevisionId
    """,
      ActivityGroup::class.java,
    ).setParameter("activityRevisionId", activityRevision.id)
      .resultList
  }

  private fun getModifiedEntitiesForGroup(group: ActivityGroup): MutableList<ActivityModifiedEntity> {
    return entityManager.createQuery(
      """
      select ame from ActivityModifiedEntity ame
      join fetch ame.activityRevision ar
      join fetch ar.activityGroups ag
      where ag.id = :groupId
      order by ar.timestamp
    """,
      ActivityModifiedEntity::class.java,
    ).setParameter("groupId", group.id)
      .resultList
  }
}
