package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.label.LabelService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

class TranslationLabelsControllerTest(
  @Autowired
  private var labelService: LabelService,
  @Autowired
  private var enabledFeaturesProvider: PublicEnabledFeaturesProvider,
) : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: LabelsTestData

  @BeforeEach
  fun setup() {
    testData = LabelsTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_LABELS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get labels`() {
    performProjectAuthGet("labels").andAssertThatJson {
      node("_embedded.labels") {
        isArray.hasSize(5)
        node("[0].id").isValidId
        node("[0].name").isString.isEqualTo("First label")
        node("[0].color").isString.isEqualTo("#FF0000")
        node("[0].description").isString.isEqualTo("This is a description")
      }
      node("page.totalElements").isNumber.isEqualTo(5.toBigDecimal())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot get labels when feature is not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthGet("labels").andIsBadRequest.andHasErrorMessage(
      Message.FEATURE_NOT_ENABLED,
    )
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
    performProjectAuthGet(
      "labels/ids?id=${testData.firstLabel.id}&id=${testData.unassignedLabel.id}",
    ).andAssertThatJson {
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
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `creates label`() {
    val requestBody =
      mapOf<String, Any>(
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
    assert(labels.totalElements == 6.toLong())
    assert(labels.content.any { it.name == "New label" && it.color == "#00FF00" })
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `updates label`() {
    val requestBody =
      mapOf<String, Any>(
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
    assert(labels.totalElements == 5.toLong())
    assert(labels.content.any { it.name == "Updated label" && it.color == "#0000FF" })
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `does not update label with invalid color`() {
    val requestBody =
      mapOf<String, Any>(
        "name" to "Updated label",
        "description" to "This is an updated label",
        "color" to "#ZZZZZZ",
      )

    performProjectAuthPut("labels/${testData.firstLabel.id}", requestBody)
      .andAssertError.isStandardValidation
      .onField("color")
      .isEqualTo("hex color required")
    val label = labelService.find(testData.firstLabel.id).orElse(null)
    assert(label.name == "First label")
    assert(label.description == "This is a description")
    assert(label.color == "#FF0000")
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `deletes label`() {
    performProjectAuthDelete("labels/${testData.firstLabel.id}").andIsOk
    labelService
      .find(testData.firstLabel.id)
      .orElse(null)
      .assert
      .isNull()
    translationService
      .find(
        testData.firstLabel.translations
          .first()
          .id,
      ).assert
      .isNotNull()
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `cannot update label of another project`() {
    val requestBody =
      mapOf<String, Any>(
        "name" to "Updated label",
        "description" to "This is an updated label",
        "color" to "#0000FF",
      )
    // just to make sure the test is not run with the same project
    projectSupplier = { testData.project }
    performProjectAuthPut("labels/${testData.secondLabel.id}", requestBody).andIsNotFound
    labelService
      .find(testData.secondLabel.id)
      .orElse(null)
      .assert
      .isNotNull()
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `cannot delete label of another project`() {
    // just to make sure the test is not run with the same project
    projectSupplier = { testData.project }
    performProjectAuthDelete("labels/${testData.secondLabel.id}").andIsNotFound
    labelService
      .find(testData.secondLabel.id)
      .orElse(null)
      .assert
      .isNotNull()
  }

  @Test
  @Transactional
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `assigns label to translation`() {
    assert(testData.unassignedTranslation.labels.isEmpty())
    assert(testData.unassignedLabel.translations.isEmpty())
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
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `unassign label from translation`() {
    assert(testData.labeledTranslation.labels.size == 1)
    assert(testData.firstLabel.translations.size == 1)
    performProjectAuthDelete(
      "translations/${testData.labeledTranslation.id}/label/${testData.firstLabel.id}",
    ).andIsOk

    val translation = translationService.get(testData.labeledTranslation.id)
    val label = labelService.find(testData.firstLabel.id).get()
    assert(translation.labels.isEmpty())
    assert(label.translations.isEmpty())
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `cannot create label with assign permission`() {
    val requestBody =
      mapOf<String, Any>(
        "name" to "New label",
        "description" to "This is a new label",
        "color" to "#00FF00",
      )

    performProjectAuthPost("labels", requestBody).andIsForbidden
    val labels = labelService.getProjectLabels(testData.projectBuilder.self.id, Pageable.unpaged())
    assert(labels.totalElements == 5.toLong())
    assert(labels.content.none { it.name == "New label" && it.color == "#00FF00" })
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `cannot update label with assign permission`() {
    val requestBody =
      mapOf<String, Any>(
        "name" to "Updated label",
        "description" to "This is an updated label",
        "color" to "#0000FF",
      )

    performProjectAuthPut("labels/${testData.firstLabel.id}", requestBody).andIsForbidden
    val labels = labelService.getProjectLabels(testData.projectBuilder.self.id, Pageable.unpaged())
    assert(labels.totalElements == 5.toLong())
    assert(labels.content.any { it.name == "First label" && it.color == "#FF0000" })
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `cannot delete label with assign permission`() {
    performProjectAuthDelete("labels/${testData.firstLabel.id}").andIsForbidden
    labelService
      .find(testData.firstLabel.id)
      .orElse(null)
      .assert
      .isNotNull()
    translationService
      .find(
        testData.firstLabel.translations
          .first()
          .id,
      ).assert
      .isNotNull()
  }

  @Test
  @Transactional
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `cannot assign label to translation with manage permission only`() {
    performProjectAuthPut(
      "translations/${testData.unassignedTranslation.id}/label/${testData.unassignedLabel.id}",
    ).andIsForbidden

    val translation = translationService.get(testData.unassignedTranslation.id)
    val label = labelService.find(testData.unassignedLabel.id).get()
    assert(translation.labels.isEmpty())
    assert(label.translations.isEmpty())
  }

  @Test
  @Transactional
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_MANAGE],
  )
  fun `cannot unassign label from translation with manage permission only`() {
    performProjectAuthDelete(
      "translations/${testData.labeledTranslation.id}/label/${testData.firstLabel.id}",
    ).andIsForbidden

    val translation = translationService.get(testData.labeledTranslation.id)
    val label = labelService.find(testData.firstLabel.id).get()
    assert(translation.labels.size == 1)
    assert(label.translations.size == 1)
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATION_LABEL_ASSIGN],
  )
  fun `assign label to empty translation`() {
    val requestBody =
      mapOf<String, Any>(
        "keyId" to testData.keyWithoutCzTranslation.id,
        "languageId" to testData.czechLanguage.id,
        "labelId" to testData.firstLabel.id,
      )
    performProjectAuthPut("translations/label", requestBody).andIsOk
    executeInNewTransaction {
      val translation = translationService.find(testData.keyWithoutCzTranslation, testData.czechLanguage).get()
      translation.assert.isNotNull
      translation.labels.assert.hasSize(1)
      translation.labels
        .first()
        .assert
        .isEqualTo(testData.firstLabel)
    }
  }
}
