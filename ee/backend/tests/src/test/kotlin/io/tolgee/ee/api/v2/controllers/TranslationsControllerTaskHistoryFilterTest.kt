package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TranslationsControllerTaskHistoryFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private fun saveTestData(tasksEnabled: Boolean = true) {
    enabledFeaturesProvider.forceEnabled = if (tasksEnabled) setOf(Feature.TASKS) else emptySet()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  private fun initTestData() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns keys never in a task in the language`() {
    initTestData()
    saveTestData()

    performProjectAuthGet("/translations?filterHasNoTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns keys that were in a task in the language`() {
    initTestData()
    saveTestData()

    performProjectAuthGet("/translations?filterHasTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 0")
        node("[1].keyName").isEqualTo("key 1")
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `scopes task history to the selected language`() {
    initTestData()
    testData.addKeyWithOwnTask("czech only", number = 10, taskLanguage = testData.czechLanguage)
    saveTestData()

    performProjectAuthGet("/translations?filterHasNoTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") { isArray.hasSize(3) }
      node("page.totalElements").isEqualTo(3)
    }

    performProjectAuthGet("/translations?filterHasNoTaskInLang=cs").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }

    performProjectAuthGet("/translations?filterHasTaskInLang=cs").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[2].keyName").isEqualTo("czech only")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `counts canceled and finished tasks as history`() {
    initTestData()
    testData.addKeyWithOwnTask("canceled only", number = 10, state = TaskState.CANCELED)
    testData.addKeyWithOwnTask("finished only", number = 11, state = TaskState.FINISHED)
    saveTestData()

    performProjectAuthGet("/translations?filterHasNoTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }

    performProjectAuthGet("/translations?filterHasTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") { isArray.hasSize(4) }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `counts review tasks as history`() {
    initTestData()
    testData.addKeyWithOwnTask("review only", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet("/translations?filterHasNoTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `matches only keys untouched in every one of several languages`() {
    initTestData()
    testData.addKeyWithOwnTask("czech only", number = 10, taskLanguage = testData.czechLanguage)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasNoTaskInLang=en&filterHasNoTaskInLang=cs",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `keeps the two directions disjoint across several languages`() {
    initTestData()
    testData.addKeyWithOwnTask("czech only", number = 10, taskLanguage = testData.czechLanguage)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasTaskInLang=en&filterHasTaskInLang=cs" +
        "&filterHasNoTaskInLang=en&filterHasNoTaskInLang=cs",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `matches keys tasked in any of several languages`() {
    initTestData()
    testData.addKeyWithOwnTask("czech only", number = 10, taskLanguage = testData.czechLanguage)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasTaskInLang=en&filterHasTaskInLang=cs",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[2].keyName").isEqualTo("czech only")
      }
      node("page.totalElements").isEqualTo(3)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `intersects with other filters rather than unioning`() {
    initTestData()
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasNoTaskInLang=en&filterState=en,TRANSLATED",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns nothing when both directions are combined`() {
    initTestData()
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasTaskInLang=en&filterHasNoTaskInLang=en",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `select-all returns the same keys as the list`() {
    initTestData()
    saveTestData()

    performProjectAuthGet(
      "/translations/select-all?languages=en&languages=cs&filterHasNoTaskInLang=en",
    ).andIsOk.andAssertThatJson {
      node("ids") { isArray.hasSize(2) }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `drops unresolvable language tags`() {
    initTestData()
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasNoTaskInLang=en&filterHasNoTaskInLang=xx",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `ignores the filter when the tasks feature is disabled`() {
    initTestData()
    saveTestData(tasksEnabled = false)

    performProjectAuthGet("/translations?filterHasNoTaskInLang=en").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(4)
    }
  }
}
