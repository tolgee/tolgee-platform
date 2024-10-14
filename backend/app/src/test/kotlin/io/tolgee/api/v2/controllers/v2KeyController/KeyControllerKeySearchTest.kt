package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeySearchTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.fixtures.retry
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
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
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it searches for prefix`() {
    saveAndPrepare()
    performProjectAuthGet("keys/search?search=thi&languageTag=de").andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
        node("[0].name").isEqualTo("this-is-key")
        node("[1].name").isEqualTo("this-is-key-2")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `pageable sort is ignored`() {
    saveAndPrepare()
    performProjectAuthGet("keys/search?search=thi&languageTag=de&sort=id")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].name").isEqualTo("this-is-key")
      }

    performProjectAuthGet("keys/search?search=thi&languageTag=de&sort=id,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].name").isEqualTo("this-is-key")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search is not slow`() {
    executeInNewTransaction {
      (1..5000).forEach {
        testData.addRandomKey()
      }
      saveAndPrepare()
    }

    retry(retries = 5, exceptionMatcher = {
      it is AssertionError
    }) {
      executeInNewTransaction {
        val time =
          measureTimeMillis {
            performProjectAuthGet("keys/search?search=Hello&languageTag=de").andAssertThatJson {
              node("page.totalElements").isEqualTo(1)
            }.andPrettyPrint
          }

        logger.info("Completed in: $time ms")
        time.assert.isLessThan(4000)
      }

      executeInNewTransaction {
        val time =
          measureTimeMillis {
            performProjectAuthGet("keys/search?search=dol&languageTag=de").andAssertThatJson {
              node("page.totalElements").isNumber.isGreaterThan(4000.toBigDecimal())
            }.andPrettyPrint
          }

        logger.info("Completed in: $time ms")
        time.assert.isLessThan(4000)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes accents`() {
    saveAndPrepare()

    executeInNewTransaction {
      val time =
        measureTimeMillis {
          performProjectAuthGet("keys/search?search=krasa&languageTag=de").andAssertThatJson {
            node("_embedded.keys") {
              isArray.hasSize(1)
              node("[0].name").isEqualTo("beauty")
            }
          }.andPrettyPrint
        }

      logger.info("Completed in: $time ms")
      time.assert.isLessThan(4000)
    }
  }
}
