package io.tolgee.api.v2.controllers.labels

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.isValidId
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.tolgee.fixtures.node


class LabelsControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: LabelsTestData

  @BeforeEach
  fun setup() {
    testData = LabelsTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns first label`() {
    performProjectAuthGet("labels").andAssertThatJson {
      node("_embedded.labels") {
        isArray.hasSize(1)
        node("[0].id").isValidId
        node("[0].name").isString.isEqualTo("First label")
        node("[0].color").isString.isEqualTo("#FF0000")
        node("[0].description").isString.isEqualTo("This is a description")
      }
      node("page.totalElements").isNumber.isEqualTo(1.toBigDecimal())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates label`() {
    val requestBody = mapOf<String, Any>(
      "name" to "New label",
      "description" to "This is a new label",
      "color" to "#00FF00",
    )

    performProjectAuthPost("labels", requestBody).andAssertThatJson {
      node("name").isString.isEqualTo("New label")
      node("description").isString.isEqualTo("This is a new label")
      node("color").isString.isEqualTo("#00FF00")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates label`() {
    val requestBody = mapOf<String, Any>(
      "name" to "Updated label",
      "description" to "This is an updated label",
      "color" to "#0000FF",
    )

    performProjectAuthPut("labels/${testData.firstLabel.id}", requestBody).andAssertThatJson {
      node("name").isString.isEqualTo("Updated label")
      node("description").isString.isEqualTo("This is an updated label")
      node("color").isString.isEqualTo("#0000FF")
    }
  }
}
