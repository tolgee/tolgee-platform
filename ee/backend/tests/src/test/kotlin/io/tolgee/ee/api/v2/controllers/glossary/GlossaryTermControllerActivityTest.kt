package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryTermWithTranslationRequest
import io.tolgee.ee.data.glossary.DeleteMultipleGlossaryTermsRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryTermWithTranslationRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.glossary.GlossaryTerm
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
class GlossaryTermControllerActivityTest : AuthorizedControllerTest() {
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
  fun `stores activity for glossary term creation`() {
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term Description"
        flagNonTranslatable = true
        flagCaseSensitive = true
        text = "New Term"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify that activity was recorded
      latestActivityRevision.assert.isNotNull()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_CREATE)

      // Verify organization ID
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify that at least one entity was modified
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.assert.isNotEmpty()

      // Verify that a GlossaryTerm entity was created
      val termEntity = modifiedEntities.find { it.entityClass == GlossaryTerm::class.simpleName }
      termEntity.assert.isNotNull()

      // Verify that the entity has modifications
      val termModifications = termEntity!!.modifications
      termModifications.assert.isNotEmpty()

      // Verify that the entity was created (has new values but no old values)
      termModifications.values
        .any { it.new != null }
        .assert
        .isTrue()

      // Verify that the right fields were stored in modifications according to annotations
      termModifications["description"]?.new.assert.isEqualTo("New Term Description")
      termModifications["flagNonTranslatable"]?.new.assert.isEqualTo(true)
      termModifications["flagCaseSensitive"]?.new.assert.isEqualTo(true)

      // Verify that we can find the glossary in the describingRelations
      val describingRelations = latestActivityRevision.describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)
    }
  }

  @Test
  fun `stores activity for glossary term update`() {
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Description"
        flagNonTranslatable = true
        flagCaseSensitive = true
        text = "Updated Term"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify that activity was recorded
      latestActivityRevision.assert.isNotNull()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_UPDATE)

      // Verify organization ID
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify that at least one entity was modified
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.assert.isNotEmpty()

      // Verify that a GlossaryTerm entity was updated
      val termEntity = modifiedEntities.find { it.entityClass == GlossaryTerm::class.simpleName }
      termEntity.assert.isNotNull()

      // Verify that the entity has modifications
      val termModifications = termEntity!!.modifications
      termModifications.assert.isNotEmpty()

      // Verify that the entity was updated (has both old and new values)
      termModifications.values
        .any { it.old != null && it.new != null }
        .assert
        .isTrue()

      // Verify that the right fields were stored in modifications according to annotations
      termModifications["description"]?.new.assert.isEqualTo("Updated Description")
      termModifications["flagNonTranslatable"]?.new.assert.isEqualTo(true)
      termModifications["flagCaseSensitive"]?.new.assert.isEqualTo(true)

      // Verify that we can find the glossary in the describingRelations
      val describingRelations = latestActivityRevision.describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)
    }
  }

  @Test
  fun `stores activity for glossary term deletion`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_DELETE)

      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.filter { it.entityClass == GlossaryTerm::class.simpleName }.assert.hasSize(1)

      val termEntity = modifiedEntities.find { it.entityClass == GlossaryTerm::class.simpleName }
      termEntity.assert.isNotNull()

      // Check that the entity was deleted (has old values but no new values)
      val describingRelations = latestActivityRevision.describingRelations
      val termRelation = describingRelations.find { it.entityClass == GlossaryTerm::class.simpleName }
      termRelation.assert.isNotNull()

      // Verify the term ID matches
      termRelation!!.entityId.assert.isEqualTo(testData.term.id)

      // Verify that we can find the glossary in the describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)

      // Verify that the right fields were stored in modifications
      val termModifications = termEntity!!.modifications
      termModifications.assert.isNotEmpty()

      // For deletion, we should have old values but no new values
      termModifications.values
        .any { it.old != null && it.new == null }
        .assert
        .isTrue()

      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)
    }
  }

  @Test
  fun `stores activity for multiple glossary terms deletion`() {
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id, testData.trademarkTerm.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_TERM_DELETE)

      val modifiedEntities = latestActivityRevision.modifiedEntities

      // Verify that two GlossaryTerm entities were deleted
      modifiedEntities.filter { it.entityClass == GlossaryTerm::class.simpleName }.assert.hasSize(2)

      // Verify organization ID
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify that we can find the glossary in the describingRelations
      val describingRelations = latestActivityRevision.describingRelations
      val glossaryRelation = describingRelations.find { it.entityClass == "Glossary" }
      glossaryRelation.assert.isNotNull()
      glossaryRelation!!.entityId.assert.isEqualTo(testData.glossary.id)

      // Verify that the right fields were stored in modifications for both terms
      modifiedEntities.filter { it.entityClass == GlossaryTerm::class.simpleName }.forEach { termEntity ->
        val termModifications = termEntity.modifications
        termModifications.assert.isNotEmpty()

        // For deletion, we should have old values but no new values
        termModifications.values
          .any { it.old != null && it.new == null }
          .assert
          .isTrue()
      }
    }
  }

  private fun getLatestActivityRevision(): ActivityRevision =
    entityManager
      .createQuery("from ActivityRevision ar order by ar.timestamp desc ", ActivityRevision::class.java)
      .setMaxResults(1)
      .singleResult
}
