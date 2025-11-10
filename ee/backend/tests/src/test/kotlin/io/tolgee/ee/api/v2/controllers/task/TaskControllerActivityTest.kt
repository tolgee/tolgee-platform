package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ActivityTestUtil
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.PropertyModification
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.CreateTaskRequest
import io.tolgee.ee.data.task.UpdateTaskKeyRequest
import io.tolgee.ee.data.task.UpdateTaskKeysRequest
import io.tolgee.ee.data.task.UpdateTaskRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addDays
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.reflect.KClass

@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
    "spring.jpa.show-sql=true",
  ],
)
@ContextRecreatingTest
class TaskControllerActivityTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var activityUtil: ActivityTestUtil

  @Autowired
  private lateinit var sessionFactory: SessionFactory

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `scope change is logged`() {
    saveTestDataAndPrepare()
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys",
      UpdateTaskKeysRequest(
        addKeys = testData.keysOutOfTask.map { it.self.id }.toMutableSet(),
        removeKeys = testData.keysInTask.map { it.self.id }.toMutableSet(),
      ),
    ).andIsOk

    executeInNewTransaction {
      val latestRevision = activityUtil.getLastRevision()
      val modifiedEntities = latestRevision!!.modifiedEntities
      val taskKeyModifications = modifiedEntities.filter { it.entityClass == TaskKey::class.simpleName }
      taskKeyModifications.assert.hasSize(4)
      taskKeyModifications.assert.anyMatch {
        it.hasKeyAndTaskDescription() && it.revisionType.isAdd()
      }
      taskKeyModifications.assert.anyMatch {
        it.hasKeyAndTaskDescription() && it.revisionType.isDel()
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `large scope change is doesn't cause n+1 issue`() {
    testData.createManyOutOfTaskKeys()
    saveTestDataAndPrepare()

    val (_, statementCount) =
      withStatementCount {
        performProjectAuthPut(
          "tasks/${testData.translateTask.self.number}/keys",
          UpdateTaskKeysRequest(
            addKeys = testData.keysOutOfTask.map { it.self.id }.toMutableSet(),
            removeKeys = testData.keysInTask.map { it.self.id }.toMutableSet(),
          ),
        ).andIsOk
      }

    statementCount.assert.isLessThan(50)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `logs task update`() {
    saveTestDataAndPrepare()
    val newDueDate = currentDateProvider.date.addDays(10)
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}",
      UpdateTaskRequest(
        name = "Updated task",
        description = "updated description",
        assignees = mutableSetOf(testData.user.id),
        dueDate = newDueDate.time,
      ),
    ).andIsOk

    executeInNewTransaction {
      val (old, new) = getLastRevisionTaskAssignees()
      old.assert.containsExactlyInAnyOrder(
        testData.projectUser.self.id,
        testData.user.id,
      )
      new.assert.containsExactlyInAnyOrder(testData.user.id)

      val mods = getLastRevisionModificationsOfType(Task::class).single().modifications
      mods["name"]!!.new.assert.isEqualTo("Updated task")
      mods["description"]!!.new.assert.isEqualTo("updated description")
      mods["dueDate"]!!.new.assert.isEqualTo(newDueDate.time)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `logs assignees when creating task`() {
    saveTestDataAndPrepare()
    val keys = testData.keysOutOfTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks",
      CreateTaskRequest(
        name = "Another task",
        description = "...",
        type = TaskType.TRANSLATE,
        languageId = testData.englishLanguage.id,
        assignees =
          mutableSetOf(
            testData.orgMember.self.id,
          ),
        keys = keys,
      ),
    )

    executeInNewTransaction {
      val (old, new) = getLastRevisionTaskAssignees()
      old.assert.isNull()
      new.assert.containsExactlyInAnyOrder(testData.orgMember.self.id)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `logs task key done`() {
    saveTestDataAndPrepare()
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys/${testData.keysInTask.first().self.id}",
      UpdateTaskKeyRequest(done = true),
    ).andIsOk

    executeInNewTransaction {
      getLastRevisionModificationsOfType(TaskKey::class)
        .single()
        .modifications["done"]!!
        .new!!
        .assert
        .isEqualTo(true)
    }
  }

  fun PropertyModification.toLongLists(): Pair<List<Long>?, List<Long>> {
    return (this.old as List<Int>?)?.map { it.toLong() } to (this.new as List<Int>).map { it.toLong() }
  }

  private fun saveTestDataAndPrepare() {
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  fun <T> withStatementCount(fn: () -> T): Pair<T, Long> {
    sessionFactory.statistics.clear()
    val result = fn()
    return result to sessionFactory.statistics.prepareStatementCount
  }

  private fun ActivityModifiedEntity.hasKeyAndTaskDescription(): Boolean {
    return this.describingRelations?.get("key") != null && this.describingRelations?.get("task") != null
  }

  private fun getLastRevisionTaskAssignees(): Pair<List<Long>?, List<Long>> {
    val assigneesModification =
      getLastRevisionModificationsOfType(Task::class).single().modifications["assignees"]!!

    return assigneesModification.toLongLists()
  }

  private fun getLastRevisionModificationsOfType(clss: KClass<out EntityWithId>): List<ActivityModifiedEntity> {
    val latestRevision = activityUtil.getLastRevision()
    val modifiedEntities = latestRevision!!.modifiedEntities
    return modifiedEntities.filter { it.entityClass == clss.simpleName }
  }
}
