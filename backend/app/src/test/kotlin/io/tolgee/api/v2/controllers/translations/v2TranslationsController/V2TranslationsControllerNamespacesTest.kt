package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerNamespacesTest : ProjectAuthControllerTest("/v2/projects/") {

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
        node("[1].keyNamespace").isEqualTo("ns-1")
        node("[2].keyNamespace").isEqualTo("ns-2")
        node("[3].keyNamespace").isEqualTo(null)
        node("[4].keyNamespace").isEqualTo("ns-1")
      }
    }
  }
}
