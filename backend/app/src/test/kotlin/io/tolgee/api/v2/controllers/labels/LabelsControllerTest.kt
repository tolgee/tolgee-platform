package io.tolgee.api.v2.controllers.labels

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import io.tolgee.fixtures.*
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import jakarta.transaction.Transactional
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
        isArray.hasSize(2)
        node("[0].id").isValidId
        node("[0].name").isString.isEqualTo("First label")
        node("[0].color").isString.isEqualTo("#FF0000")
        node("[0].description").isString.isEqualTo("This is a description")
      }
      node("page.totalElements").isNumber.isEqualTo(2.toBigDecimal())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get labels with search`() {
    performProjectAuthGet("labels?search=First").andAssertThatJson {
      node("_embedded.labels") {
        isArray.hasSize(1)
        node("[0].id").isValidId
        node("[0].name").isString.isEqualTo("First label")
      }
      node("page.totalElements").isNumber.isEqualTo(1.toBigDecimal())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get labels by ids`() {
    performProjectAuthGet("labels/ids?id=${testData.firstLabel.id}&id=${testData.unassignedLabel.id}").andAssertThatJson {
      isArray.hasSize(2)
      node("[0].id").isValidId
      node("[0].name").isString.isEqualTo("First label")
      node("[0].color").isString.isEqualTo("#FF0000")
      node("[0].description").isString.isEqualTo("This is a description")
      node("[1].id").isValidId
      node("[1].name").isString.isEqualTo("Unassigned label")
      node("[1].color").isString.isEqualTo("#00FF00")
      node("[1].description").isString.isEqualTo("This is a description for unassigned label")
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
    assert(labels.totalElements == 3.toLong())
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
    assert(labels.totalElements == 2.toLong())
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
    translationService.find(testData.firstLabel.translations.first().id).assert.isNotNull()
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

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `assigns label to translation`() {
    assert(testData.unassignedTranslation.labels.size == 0)
    assert(testData.unassignedLabel.translations.size == 0)
    performProjectAuthPut(
      "translations/${testData.unassignedTranslation.id}/label/${testData.unassignedLabel.id}",
    ).andIsOk

    val translation = translationService.get(testData.unassignedTranslation.id)
    val label = labelService.find(testData.unassignedLabel.id).get()
    assert(translation.labels.size == 1)
    assert(label.translations.size == 1)
    assert(translation.labels.first().id == label.id)
    assert(label.translations.first().id == translation.id)
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `unassign label from translation`() {
    assert(testData.labeledTranslation.labels.size == 1)
    assert(testData.firstLabel.translations.size == 1)
    performProjectAuthDelete(
      "translations/${testData.labeledTranslation.id}/label/${testData.firstLabel.id}",
    ).andIsOk

    val translation = translationService.get(testData.labeledTranslation.id)
    val label = labelService.find(testData.firstLabel.id).get()
    assert(translation.labels.size == 0)
    assert(label.translations.size == 0)
  }
}
