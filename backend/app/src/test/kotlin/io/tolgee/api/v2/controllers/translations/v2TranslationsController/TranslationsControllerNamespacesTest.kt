package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerNamespacesTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns namespaces`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/translations?sort=id").andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(5))
      node("_embedded.keys") {
        isArray.hasSize(5)
        node("[0].keyNamespace").isEqualTo(null)
        node("[1].keyNamespaceId").isValidId
        node("[1].keyNamespace").isEqualTo("ns-1")
        node("[2].keyNamespace").isEqualTo("ns-2")
        node("[3].keyNamespace").isEqualTo(null)
        node("[4].keyNamespace").isEqualTo("ns-1")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates key in namespace on set or create`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        key = "new_key_in_ns",
        namespace = "heloo",
        translations = mapOf("en" to "eeeen"),
      ),
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("keyNamespace").isEqualTo("heloo")
    }
    namespaceService.find("heloo", project.id).assert.isNotNull
  }
}
