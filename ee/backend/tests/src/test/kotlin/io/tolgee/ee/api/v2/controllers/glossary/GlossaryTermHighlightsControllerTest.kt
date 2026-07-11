package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryTermHighlightsControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: GlossaryTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userOwner
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `gets highlights for text containing glossary terms`() {
    // Text contains "Term" which is in the glossary
    val text = "This is a Term that should be highlighted"

    val result =
      performAuthPost(
        "/v2/projects/${testData.project.id}/glossary-highlights",
        mapOf(
          "languageTag" to "en",
          "text" to text,
        ),
      )

    result.andIsOk.andAssertThatJson {
      node("_embedded.glossaryHighlights") {
        isArray.hasSize(1)
        node("[0].position.start").isNumber.isEqualTo(BigDecimal(10))
        node("[0].position.end").isNumber.isEqualTo(BigDecimal(14))
        node("[0].value.id").isValidId
        node("[0].value.description").isEqualTo("The description")
      }
    }
  }

  @Test
  fun `gets highlights for text containing multiple glossary terms`() {
    // Text contains "Term" and "fun" which are in the glossary
    val text = "This is a Term that is fun to use"

    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryHighlights").isArray.hasSize(2)
      }
  }

  @Test
  fun `gets highlights for text containing case sensitive glossary terms`() {
    // Text contains "Apple", which is case-sensitive in the glossary
    val text = "I like Apple products"

    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded") {
          node("glossaryHighlights") {
            isArray.hasSize(1)
            node("[0].position.start").isNumber.isEqualTo(BigDecimal(7))
            node("[0].position.end").isNumber.isEqualTo(BigDecimal(12))
            node("[0].value.id").isValidId
            node("[0].value.description").isEqualTo("Trademark")
          }
        }
      }
  }

  @Test
  fun `does not highlight case-sensitive terms with wrong case`() {
    // Text contains "apple" (lowercase) which should not match "Apple" (case-sensitive)
    val text = "I like apple products"
    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded").isAbsent()
      }
  }

  @Test
  fun `highlights noncase-sensitive terms with different case`() {
    // Text contains "Term" which is in the glossary, but with a different case
    val text = "This is a term that should be highlighted"
    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryHighlights") {
          isArray.hasSize(1)
          node("[0].position.start").isNumber.isEqualTo(BigDecimal(10))
          node("[0].position.end").isNumber.isEqualTo(BigDecimal(14))
          node("[0].value.id").isValidId
          node("[0].value.description").isEqualTo("The description")
        }
      }
  }

  @Test
  fun `gets highlights for text containing multiword glossary terms`() {
    // Text contains "Term" which is in the glossary
    val text = "Lets work for A.B.C Inc!"

    val result =
      performAuthPost(
        "/v2/projects/${testData.project.id}/glossary-highlights",
        mapOf(
          "languageTag" to "en",
          "text" to text,
        ),
      )

    result.andIsOk.andAssertThatJson {
      node("_embedded.glossaryHighlights") {
        isArray.hasSize(1)
        node("[0].position.start").isNumber.isEqualTo(BigDecimal(14))
        node("[0].position.end").isNumber.isEqualTo(BigDecimal(23))
        node("[0].value.id").isValidId
        node("[0].value.description").isEqualTo("The multiword term")
      }
    }
  }

  @Test
  fun `gets empty response for text without glossary terms`() {
    val text = "This text does not contain any glossary terms"
    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded").isAbsent()
      }
  }

  @Test
  fun `does not get highlights when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val text = "This is a Term that should be highlighted"
    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsBadRequest
  }
}
