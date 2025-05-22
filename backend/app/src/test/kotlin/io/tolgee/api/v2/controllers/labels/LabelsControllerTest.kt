package io.tolgee.api.v2.controllers.labels

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import io.tolgee.fixtures.*
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable


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
  fun `get labels`() {
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
    val labels = labelService.getProjectLabels(testData.projectBuilder.self.id, Pageable.unpaged())
    assert(labels.totalElements == 2.toLong())
    assert(labels.content.any { it.name == "New label" && it.color == "#00FF00" })
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
    val labels = labelService.getProjectLabels(testData.projectBuilder.self.id, Pageable.unpaged())
    assert(labels.totalElements == 1.toLong())
    assert(labels.content.any { it.name == "Updated label" && it.color == "#0000FF" })
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `update label with invalid color`() {
    val requestBody = mapOf<String, Any>(
      "name" to "Updated label",
      "description" to "This is an updated label",
      "color" to "#ZZZZZZ",
    )

    performProjectAuthPut("labels/${testData.firstLabel.id}", requestBody)
      .andAssertError.isStandardValidation.onField("color").isEqualTo("hex color required")
    val label = labelService.find(testData.firstLabel.id).orElse(null)
    assert(label.name == "First label")
    assert(label.description == "This is a description")
    assert(label.color == "#FF0000")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes label`() {
    performProjectAuthDelete("labels/${testData.firstLabel.id}").andIsOk
    labelService.find(testData.firstLabel.id).orElse(null).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot updates label of another project`() {
    val requestBody = mapOf<String, Any>(
      "name" to "Updated label",
      "description" to "This is an updated label",
      "color" to "#0000FF",
    )
    // just to make sure the test is not run with the same project
    projectSupplier = { testData.project }
    performProjectAuthPut("labels/${testData.secondLabel.id}", requestBody).andIsNotFound
    labelService.find(testData.secondLabel.id).orElse(null).assert.isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot delete label of another project`() {
    // just to make sure the test is not run with the same project
    projectSupplier = { testData.project }
    performProjectAuthDelete("labels/${testData.secondLabel.id}").andIsNotFound
    labelService.find(testData.secondLabel.id).orElse(null).assert.isNotNull()
  }
}
