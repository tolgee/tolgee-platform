package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeySearchTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import kotlin.system.measureTimeMillis

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerKeySearchTest : ProjectAuthControllerTest("/v2/projects/"), Logging {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  lateinit var testData: KeySearchTestData

  @BeforeEach
  fun setup() {
    testData = KeySearchTestData()
  }

  fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it searches`() {
    saveAndPrepare()
    performProjectAuthGet("keys/search?search=key&languageTag=de").andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].name").isEqualTo("this-is-key")
        node("[1].name").isEqualTo("this-is-key-2")
      }
    }

    performProjectAuthGet("keys/search?search=key-2&languageTag=de").andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("name").isEqualTo("this-is-key-2")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search is not slow`() {
    (1..50000).forEach {
      testData.addRandomKey()
    }
    saveAndPrepare()
    val time = measureTimeMillis {
      performProjectAuthGet("keys/search?search=lorem&languageTag=de").andAssertThatJson {
        node("page.totalElements").isEqualTo(50002)
      }
    }

    logger.info("Completed in: $time ms")
  }
}
