package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import kotlin.system.measureTimeMillis

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerDeleteManyKeysTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testData.generateLotOfData(100)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys via post fast enough`() {
    projectSupplier = { testData.project }
    val time =
      measureTimeMillis {
        performProjectAuthDelete(
          "keys",
          mapOf(
            "ids" to
              testData.root.data.projects[0]
                .data.keys
                .map { it.self.id },
          ),
        ).andIsOk
      }
    assertThat(time).isLessThan(2000)
  }
}
