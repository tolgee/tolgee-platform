package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.UserPreferencesProjectStorageTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.repository.UserPreferencesRepository
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UserPreferencesProjectStorageControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var userPreferencesRepository: UserPreferencesRepository

  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  lateinit var testData: UserPreferencesProjectStorageTestData

  @BeforeEach
  fun setup() {
    testData = UserPreferencesProjectStorageTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `returns null data when field is not set`() {
    userAccount = testData.user
    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isNull()
    }
  }

  @Test
  fun `stores and returns object with nested arrays`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
  }

  @Test
  fun `stores values of different json types`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "string"), "value").andIsOk
    performAuthPut(url(testData.project, "number"), 42).andIsOk
    performAuthPut(url(testData.project, "boolean"), false).andIsOk
    performAuthPut(url(testData.project, "array"), listOf(1, "two", mapOf("three" to 3))).andIsOk

    performAuthGet(url(testData.project, "string")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("value")
    }
    performAuthGet(url(testData.project, "number")).andIsOk.andAssertThatJson {
      node("data").isEqualTo(42)
    }
    performAuthGet(url(testData.project, "boolean")).andIsOk.andAssertThatJson {
      node("data").isEqualTo(false)
    }
    performAuthGet(url(testData.project, "array")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""[1,"two",{"three":3}]""")
    }
  }

  @Test
  fun `overwrites existing field`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk
    performAuthPut(url(testData.project, "exportSettings"), mapOf("languages" to listOf("de"))).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["de"]}""")
    }
  }

  @Test
  fun `null body removes the field and keeps the others`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk
    performAuthPut(url(testData.project, "other"), "kept").andIsOk

    performAuthPut(url(testData.project, "exportSettings"), null).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isNull()
    }
    storedProjectStorage().assert.isEqualTo(mapOf(testData.project.id.toString() to mapOf("other" to "kept")))
  }

  @Test
  fun `removing a field that was never set is a no-op`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), null).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isNull()
    }
  }

  @Test
  fun `fields of different projects are isolated`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk
    performAuthPut(url(testData.secondProject, "exportSettings"), exportSettings("cs", "b")).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
    performAuthGet(url(testData.secondProject, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["cs"],"tagsIn":["b"]}""")
    }

    performAuthPut(url(testData.secondProject, "exportSettings"), null).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
  }

  @Test
  fun `fields of the same project are independent`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "first"), "one").andIsOk
    performAuthPut(url(testData.project, "second"), "two").andIsOk
    performAuthPut(url(testData.project, "first"), "uno").andIsOk

    performAuthGet(url(testData.project, "first")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("uno")
    }
    performAuthGet(url(testData.project, "second")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("two")
    }
  }

  @Test
  fun `project storage does not touch generic storage`() {
    userAccount = testData.user
    performAuthPut("/v2/user-preferences/storage/exportSettings", "generic").andIsOk
    performAuthPut(url(testData.project, "exportSettings"), "project").andIsOk

    performAuthGet("/v2/user-preferences/storage/exportSettings").andIsOk.andAssertThatJson {
      node("data").isEqualTo("generic")
    }
    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("project")
    }

    performAuthPut("/v2/user-preferences/storage/other", "generic-other").andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("project")
    }
  }

  @Test
  fun `user with view permission can use project storage`() {
    userAccount = testData.viewer
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
  }

  @Test
  fun `user without project access gets not found`() {
    userAccount = testData.outsider
    performAuthGet(url(testData.project, "exportSettings")).andIsNotFound
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsNotFound

    storedProjectStorage(testData.outsider.id).assert.isNull()
  }

  @Test
  fun `non-existent project gets not found`() {
    userAccount = testData.user
    performAuthGet("/v2/user-preferences/project-storage/${Long.MAX_VALUE}/exportSettings").andIsNotFound
    performAuthPut("/v2/user-preferences/project-storage/${Long.MAX_VALUE}/exportSettings", "x").andIsNotFound
  }

  @Test
  fun `concurrent writes to different fields and projects are all kept`() {
    userAccount = testData.user
    val writesPerProject = 10
    val projects = listOf(testData.project, testData.secondProject)
    val writes =
      projects.flatMap { project ->
        (1..writesPerProject).map { index -> Triple(project, "field$index", "value-${project.id}-$index") }
      }

    val executor = Executors.newFixedThreadPool(writes.size)
    val start = CountDownLatch(1)
    try {
      val futures =
        writes.map { (project, fieldName, value) ->
          executor.submit {
            start.await()
            performAuthPut(url(project, fieldName), value).andIsOk
          }
        }
      start.countDown()
      futures.forEach { it.get(60, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }

    val expected =
      writes
        .groupBy({ it.first.id.toString() }, { it.second to it.third })
        .mapValues { (_, fields) -> fields.toMap() }
    storedProjectStorage().assert.isEqualTo(expected)
  }

  @Test
  fun `hard deleting a project removes its storage and keeps other projects`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk
    performAuthPut(url(testData.secondProject, "exportSettings"), exportSettings("cs", "b")).andIsOk

    executeInNewTransaction {
      projectHardDeletingService.hardDeleteProject(projectService.get(testData.secondProject.id))
    }

    storedProjectStorage().assert.isEqualTo(
      mapOf(
        testData.project.id.toString() to
          mapOf("exportSettings" to mapOf("languages" to listOf("en"), "tagsIn" to listOf("a"))),
      ),
    )
  }

  @Test
  fun `admin without project permission can use project storage`() {
    userAccount = testData.admin
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
  }

  @Test
  fun `supporter without project permission can use project storage`() {
    userAccount = testData.supporter
    performAuthPut(url(testData.project, "exportSettings"), exportSettings("en", "a")).andIsOk

    performAuthGet(url(testData.project, "exportSettings")).andIsOk.andAssertThatJson {
      node("data").isEqualTo("""{"languages":["en"],"tagsIn":["a"]}""")
    }
  }

  @Test
  fun `admin gets not found for non-existent project`() {
    userAccount = testData.admin
    performAuthGet("/v2/user-preferences/project-storage/${Long.MAX_VALUE}/exportSettings").andIsNotFound
    performAuthPut("/v2/user-preferences/project-storage/${Long.MAX_VALUE}/exportSettings", "x").andIsNotFound
  }

  @Test
  fun `project api key is rejected`() {
    performGet("/v2/api-keys/current", apiKeyHeaders(testData.firstProjectApiKey)).andIsOk
    performGet(url(testData.project, "exportSettings"), apiKeyHeaders(testData.firstProjectApiKey)).andIsForbidden
    performPut(url(testData.project, "exportSettings"), "x", apiKeyHeaders(testData.firstProjectApiKey))
      .andIsForbidden
  }

  @Test
  fun `project api key of another project is rejected`() {
    performGet(url(testData.project, "exportSettings"), apiKeyHeaders(testData.secondProjectApiKey)).andIsForbidden
    performPut(url(testData.project, "exportSettings"), "x", apiKeyHeaders(testData.secondProjectApiKey))
      .andIsForbidden
  }

  @Test
  fun `rejects invalid field names`() {
    userAccount = testData.user
    listOf("a".repeat(65), "bad*name", "bad name", "zluťoučký").forEach { fieldName ->
      performAuthPut(url(testData.project, fieldName), "x")
        .andIsBadRequest
        .andHasErrorMessage(Message.PROJECT_STORAGE_INVALID_FIELD_NAME)
    }
    storedProjectStorage().assert.isNull()
  }

  @Test
  fun `accepts field names at the limits`() {
    userAccount = testData.user
    val longest = "a".repeat(64)
    performAuthPut(url(testData.project, longest), "x").andIsOk
    performAuthPut(url(testData.project, "a"), "x").andIsOk
    performAuthPut(url(testData.project, "Export_settings-v1.2"), "x").andIsOk

    performAuthGet(url(testData.project, longest)).andIsOk.andAssertThatJson {
      node("data").isEqualTo("x")
    }
  }

  @Test
  fun `rejects values over the size limit`() {
    userAccount = testData.user
    performAuthPut(url(testData.project, "big"), "a".repeat(16 * 1024))
      .andIsBadRequest
      .andHasErrorMessage(Message.PROJECT_STORAGE_VALUE_TOO_LARGE)
    storedProjectStorage().assert.isNull()

    performAuthPut(url(testData.project, "big"), "a".repeat(16 * 1024 - 2)).andIsOk
  }

  @Test
  fun `rejects new fields over the per-project limit`() {
    userAccount = testData.user
    (1..50).forEach { performAuthPut(url(testData.project, "field$it"), it).andIsOk }

    performAuthPut(url(testData.project, "field51"), 51)
      .andIsBadRequest
      .andHasErrorMessage(Message.PROJECT_STORAGE_TOO_MANY_FIELDS)

    performAuthPut(url(testData.project, "field1"), "overwritten").andIsOk
    performAuthPut(url(testData.secondProject, "field51"), 51).andIsOk

    performAuthPut(url(testData.project, "field2"), null).andIsOk
    performAuthPut(url(testData.project, "field51"), 51).andIsOk

    @Suppress("UNCHECKED_CAST")
    val projectFields = storedProjectStorage()!![testData.project.id.toString()] as Map<String, Any>
    projectFields.assert.hasSize(50)
    projectFields.assert.containsEntry("field1", "overwritten")
    projectFields.assert.doesNotContainKey("field2")
  }

  private fun apiKeyHeaders(apiKey: ApiKey) =
    HttpHeaders().apply {
      add("X-API-Key", apiKey.key)
    }

  private fun url(
    project: Project,
    fieldName: String,
  ) = "/v2/user-preferences/project-storage/${project.id}/$fieldName"

  private fun exportSettings(
    language: String,
    tag: String,
  ) = mapOf("languages" to listOf(language), "tagsIn" to listOf(tag))

  private fun storedProjectStorage(userAccountId: Long = testData.user.id): Map<String, Any>? {
    return executeInNewTransaction {
      userPreferencesRepository.findById(userAccountId).orElse(null)?.projectStorageJson
    }
  }
}
