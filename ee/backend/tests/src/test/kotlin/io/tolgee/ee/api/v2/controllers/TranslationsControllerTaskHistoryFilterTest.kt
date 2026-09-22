package io.tolgee.ee.api.v2.controllers

import com.fasterxml.jackson.databind.ObjectMapper
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

  private fun saveTestData(features: Set<Feature> = setOf(Feature.TASKS)) {
    enabledFeaturesProvider.forceEnabled = features
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

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
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

    performProjectAuthGet("/translations?filterHasBeenInTaskInLang=en").andIsOk.andAssertThatJson {
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

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") { isArray.hasSize(3) }
      node("page.totalElements").isEqualTo(3)
    }

    performProjectAuthGet("/translations?filterNeverInTaskInLang=cs").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }

    performProjectAuthGet("/translations?filterHasBeenInTaskInLang=cs").andIsOk.andAssertThatJson {
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

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }

    performProjectAuthGet("/translations?filterHasBeenInTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") { isArray.hasSize(4) }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `counts review tasks as history`() {
    initTestData()
    testData.addKeyWithOwnTask("review only", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
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
      "/translations?filterNeverInTaskInLang=en&filterNeverInTaskInLang=cs",
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
      "/translations?filterHasBeenInTaskInLang=en&filterHasBeenInTaskInLang=cs" +
        "&filterNeverInTaskInLang=en&filterNeverInTaskInLang=cs",
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
      "/translations?filterHasBeenInTaskInLang=en&filterHasBeenInTaskInLang=cs",
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
      "/translations?filterNeverInTaskInLang=en&filterState=en,TRANSLATED",
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
      "/translations?filterHasBeenInTaskInLang=en&filterNeverInTaskInLang=en",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `select-all returns the same keys as the list`() {
    initTestData()
    saveTestData()

    val listResponse = performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andReturn()
    val listedIds =
      ObjectMapper()
        .readTree(listResponse.response.contentAsString)
        .at("/_embedded/keys")
        .map { it.get("keyId").asLong() }

    performProjectAuthGet(
      "/translations/select-all?languages=en&languages=cs&filterNeverInTaskInLang=en",
    ).andIsOk.andAssertThatJson {
      node("ids").isEqualTo(listedIds)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `drops unresolvable language tags`() {
    initTestData()
    saveTestData()

    performProjectAuthGet(
      "/translations?filterNeverInTaskInLang=en&filterNeverInTaskInLang=xx",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns keys not in an open task in the language`() {
    initTestData()
    saveTestData()

    performProjectAuthGet("/translations?filterNotInOpenTaskInLang=en").andIsOk.andAssertThatJson {
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
  fun `counts only NEW and IN_PROGRESS tasks as open`() {
    initTestData()
    testData.addKeyWithOwnTask("finished only", number = 10, state = TaskState.FINISHED)
    testData.addKeyWithOwnTask("canceled only", number = 11, state = TaskState.CANCELED)
    saveTestData()

    performProjectAuthGet("/translations?filterNotInOpenTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(4)
        node("[2].keyName").isEqualTo("finished only")
        node("[3].keyName").isEqualTo("canceled only")
      }
      node("page.totalElements").isEqualTo(4)
    }

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `counts open tasks of both types`() {
    initTestData()
    testData.addKeyWithOwnTask("open review", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet("/translations?filterNotInOpenTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `scopes open tasks to the selected language`() {
    initTestData()
    testData.addKeyWithOwnTask("czech only", number = 10, taskLanguage = testData.czechLanguage)
    saveTestData()

    performProjectAuthGet("/translations?filterNotInOpenTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[2].keyName").isEqualTo("czech only")
      }
    }

    performProjectAuthGet("/translations?filterNotInOpenTaskInLang=cs").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `narrows task history to the given task type`() {
    initTestData()
    testData.addKeyWithOwnTask("review only", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterHasBeenInTaskInLang=en&filterTaskType=TRANSLATE",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 0")
        node("[1].keyName").isEqualTo("key 1")
      }
    }

    performProjectAuthGet(
      "/translations?filterHasBeenInTaskInLang=en&filterTaskType=REVIEW",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].keyName").isEqualTo("review only")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `both task types together mean the same as no type filter`() {
    initTestData()
    testData.addKeyWithOwnTask("review only", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterNeverInTaskInLang=en&filterTaskType=TRANSLATE&filterTaskType=REVIEW",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].keyName").isEqualTo("key 2")
        node("[1].keyName").isEqualTo("key 3")
      }
    }

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys") { isArray.hasSize(2) }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `open task filter respects the task type`() {
    initTestData()
    testData.addKeyWithOwnTask("open review", number = 10, type = TaskType.REVIEW)
    saveTestData()

    performProjectAuthGet(
      "/translations?filterNotInOpenTaskInLang=en&filterTaskType=TRANSLATE",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[2].keyName").isEqualTo("open review")
      }
    }

    performProjectAuthGet(
      "/translations?filterNotInOpenTaskInLang=en&filterTaskType=REVIEW",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(4)
        node("[0].keyName").isEqualTo("key 0")
        node("[3].keyName").isEqualTo("key 3")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `applies the filter when only the order-translation feature is enabled`() {
    initTestData()
    saveTestData(features = setOf(Feature.ORDER_TRANSLATION))

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `ignores the filter when the tasks feature is disabled`() {
    initTestData()
    saveTestData(features = emptySet())

    performProjectAuthGet("/translations?filterNeverInTaskInLang=en").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(4)
    }
  }
}
