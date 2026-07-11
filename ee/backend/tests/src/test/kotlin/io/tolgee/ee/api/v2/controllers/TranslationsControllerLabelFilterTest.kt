package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TranslationsControllerLabelFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: LabelsTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = LabelsTestData()
    projectSupplier = { testData.projectBuilder.self }
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_LABELS)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by label assigned to english translation`() {
    performProjectAuthGet("/translations?filterLabel=en,${testData.firstLabel.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("first key")
        }
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not match when label is filtered for a language without that label`() {
    // firstLabel is on the EN translation of "first key", not on the CS translation
    performProjectAuthGet("/translations?filterLabel=cs,${testData.firstLabel.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not match when label belongs to a different project`() {
    // secondLabel belongs to "Second project", not to the project we are querying
    performProjectAuthGet("/translations?filterLabel=en,${testData.secondLabel.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not match when label is unassigned`() {
    // unassignedLabel exists in the project but is not assigned to any translation
    performProjectAuthGet("/translations?filterLabel=en,${testData.unassignedLabel.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `multiple labels for same language combine with OR semantics`() {
    // Assign unassignedLabel to the english translation of "second key" (post-save)
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.secondKeyEnTranslation.id)
      val label = entityManager.find(Label::class.java, testData.unassignedLabel.id)
      translation.labels.add(label)
      label.translations.add(translation)
      entityManager.flush()
    }

    // Filter for either label on EN — should return both keys
    performProjectAuthGet(
      "/translations?sort=keyName" +
        "&filterLabel=en,${testData.firstLabel.id}" +
        "&filterLabel=en,${testData.unassignedLabel.id}",
    ).andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(2)
          node("[0].keyName").isEqualTo("first key")
          node("[1].keyName").isEqualTo("second key")
        }
        node("page.totalElements").isEqualTo(2)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `label filter combines with other translation filters using OR semantics`() {
    // All translation-level filter predicates are OR-ed together at the query-build stage
    // (see `TranslationsViewQueryBuilder.getWhereConditions`). This includes filterLabel,
    // filterState, filterAutoTranslatedInLang, filterUntranslatedInLang, filterTranslatedInLang,
    // filterHas{Comments,UnresolvedComments,Suggestions}InLang, and filterOutdatedLanguage /
    // filterNotOutdatedLanguage — combining any of these must return the union, not the
    // intersection.
    // "first key" is matched by filterLabel (its EN translation has firstLabel),
    // even though its EN state is TRANSLATED, not REVIEWED.
    performProjectAuthGet(
      "/translations?filterLabel=en,${testData.firstLabel.id}&filterState=en,REVIEWED",
    ).andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("first key")
        }
        node("page.totalElements").isEqualTo(1)
      }
  }
}
