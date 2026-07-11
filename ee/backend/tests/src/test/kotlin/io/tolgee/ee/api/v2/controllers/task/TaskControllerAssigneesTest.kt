package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.core.internal.Node.JsonMap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TaskControllerAssigneesTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `properly includes user with view rights`() {
    performProjectAuthGet(
      "tasks/possible-assignees?filterMinimalScope=TRANSLATIONS_VIEW",
    ).andIsOk.andAssertThatJson {
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Project view scope user (en)")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Project view role user (en)")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Project manage role user (en)")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Organization member")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Organization owner")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `properly excludes user with view rights`() {
    performProjectAuthGet(
      "tasks/possible-assignees?filterMinimalScope=TRANSLATIONS_EDIT",
    ).andIsOk.andAssertThatJson {
      node("_embedded.users").isArray.allSatisfy {
        (it as JsonMap).get("name").assert.isNotEqualTo("Project view scope user (en)")
      }
      node("_embedded.users").isArray.allSatisfy {
        (it as JsonMap).get("name").assert.isNotEqualTo("Project view role user (en)")
      }
      node("_embedded.users").isArray.allSatisfy {
        (it as JsonMap).get("name").assert.isNotEqualTo("Organization member")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Organization owner")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `properly excludes users without view rights for english`() {
    performProjectAuthGet(
      "tasks/possible-assignees?filterMinimalScope=TRANSLATIONS_VIEW&filterViewLanguageId=${testData.czechLanguage.id}",
    ).andIsOk.andAssertThatJson {
      node("_embedded.users").isArray.allSatisfy {
        (it as JsonMap).get("name").assert.isNotEqualTo("Project view scope user (en)")
      }
      node("_embedded.users").isArray.allSatisfy {
        (it as JsonMap).get("name").assert.isNotEqualTo("Project view role user (en)")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Organization member")
      }
      node("_embedded.users").isArray.anySatisfy {
        (it as JsonMap).get("name").assert.isEqualTo("Organization owner")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `user tasks`() {
    performAuthGet(
      "/v2/user-tasks",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `user tasks filter by assignee`() {
    performAuthGet(
      "/v2/user-tasks?filterAssignee=${testData.orgMember.self.id}",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Review task")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `user tasks filter by type`() {
    performAuthGet(
      "/v2/user-tasks?filterType=${TaskType.TRANSLATE}",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Translate task")
    }
  }
}
