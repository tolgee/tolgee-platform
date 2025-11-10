package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationLanguageControllerTest : AuthorizedControllerTest() {
  @Test
  fun `gets all languages in use`() {
    val testData = PermissionsTestData()
    val organization = testData.organizationBuilder.self
    testDataService.saveTestData(testData.root)
    loginAsUser("member@member.com")

    performAuthGet("/v2/organizations/${organization.id}/languages")
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages").isArray.hasSize(3)
        // Order is important
        node("_embedded.languages[0].name").isEqualTo("English")
        node("_embedded.languages[0].tag").isEqualTo("en")
        node("_embedded.languages[0].base").isEqualTo(true)
        node("_embedded.languages[1].name").isEqualTo("Czech")
        node("_embedded.languages[1].tag").isEqualTo("cs")
        node("_embedded.languages[1].base").isEqualTo(false)
        node("_embedded.languages[2].name").isEqualTo("German")
        node("_embedded.languages[2].tag").isEqualTo("de")
        node("_embedded.languages[2].base").isEqualTo(false)
      }
  }

  @Test
  fun `gets all base languages in use`() {
    val testData = PermissionsTestData()
    val organization = testData.organizationBuilder.self
    testDataService.saveTestData(testData.root)
    loginAsUser("member@member.com")

    performAuthGet("/v2/organizations/${organization.id}/base-languages")
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages").isArray.hasSize(1)
        // Order is important
        node("_embedded.languages[0].name").isEqualTo("English")
        node("_embedded.languages[0].tag").isEqualTo("en")
        node("_embedded.languages[0].base").isEqualTo(true)
      }
  }
}
