package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.UpdateGlossaryTermTranslationRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryTermTranslationControllerActivityTest : AuthorizedControllerTest() {
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
  fun `stores activity for glossary term translation creation`() {
    val request =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "de"
        text = "Neuer Begriff"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      request,
    ).andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify that activity was recorded
      latestActivityRevision.assert.isNotNull()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_TRANSLATION_UPDATE)

      // Verify organization ID
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify that at least one entity was modified
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.assert.isNotEmpty()

      // Verify that a GlossaryTermTranslation entity was created
      val translationEntity = modifiedEntities.find { it.entityClass == GlossaryTermTranslation::class.simpleName }
      translationEntity.assert.isNotNull()

      // Verify that the entity has modifications
      val translationModifications = translationEntity!!.modifications
      translationModifications.assert.isNotEmpty()

      // Verify that the entity was created (has new values but no old values)
      translationModifications.values
        .any { it.new != null }
        .assert
        .isTrue()

      // Verify that the right fields were stored in modifications according to annotations
      translationModifications["text"]?.new.assert.isEqualTo("Neuer Begriff")

      // Verify that we can find the glossary and term in the describingRelations
      val describingRelations = latestActivityRevision.describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)

      val termRelation = describingRelations.find { it.entityClass == "GlossaryTerm" }
      termRelation.assert.isNotNull()
      termRelation!!.entityId.assert.isEqualTo(testData.term.id)
    }
  }

  @Test
  fun `stores activity for glossary term translation update`() {
    // First create a translation
    val createRequest =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "cs"
        text = "Pojem"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      createRequest,
    ).andIsOk

    // Then update it
    val updateRequest =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "cs"
        text = "Aktualizovaný pojem"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      updateRequest,
    ).andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify that activity was recorded
      latestActivityRevision.assert.isNotNull()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_TRANSLATION_UPDATE)

      // Verify organization ID
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify that at least one entity was modified
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.assert.isNotEmpty()

      // Verify that a GlossaryTermTranslation entity was updated
      val translationEntity = modifiedEntities.find { it.entityClass == GlossaryTermTranslation::class.simpleName }
      translationEntity.assert.isNotNull()

      // Verify that the entity has modifications
      val translationModifications = translationEntity!!.modifications
      translationModifications.assert.isNotEmpty()

      // Verify that the entity was updated (has both old and new values)
      translationModifications.values
        .any { it.old != null && it.new != null }
        .assert
        .isTrue()

      // Verify that the right fields were stored in modifications according to annotations
      translationModifications["text"]?.old.assert.isEqualTo("Pojem")
      translationModifications["text"]?.new.assert.isEqualTo("Aktualizovaný pojem")

      // Verify that we can find the glossary and term in the describingRelations
      val describingRelations = latestActivityRevision.describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)

      val termRelation = describingRelations.find { it.entityClass == "GlossaryTerm" }
      termRelation.assert.isNotNull()
      termRelation!!.entityId.assert.isEqualTo(testData.term.id)
    }
  }

  private fun getLatestActivityRevision(): ActivityRevision =
    entityManager
      .createQuery("from ActivityRevision ar order by ar.timestamp desc ", ActivityRevision::class.java)
      .setMaxResults(1)
      .singleResult
}
