package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
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
class GlossaryControllerActivityTest : AuthorizedControllerTest() {
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
  fun `stores activity on creation`() {
    val request =
      CreateGlossaryRequest().apply {
        name = "New Glossary"
        baseLanguageTag = "en"
        assignedProjectIds = mutableSetOf(testData.project.id)
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()

      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.size.assert.isEqualTo(1)
      val modifications = modifiedEntities.single().modifications
      modifications["name"]!!.new.assert.isEqualTo("New Glossary")
      modifications["baseLanguageTag"]!!.new.assert.isEqualTo("en")

      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_CREATE)
    }
  }

  @Test
  fun `stores activity on update`() {
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.size.assert.isEqualTo(1)
      val modifications = modifiedEntities.single().modifications
      modifications["name"]!!.old.assert.isEqualTo("Test Glossary")
      modifications["name"]!!.new.assert.isEqualTo("Updated Glossary")
      modifications["baseLanguageTag"]!!.old.assert.isEqualTo("en")
      modifications["baseLanguageTag"]!!.new.assert.isEqualTo("de")
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_UPDATE)
    }
  }

  @Test
  fun `stores activity on deletion`() {
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk

    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.size.assert.isEqualTo(11)

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_DELETE)

      val modifications = modifiedEntities.find { it.entityClass == Glossary::class.simpleName }!!.modifications
      modifications["name"]!!.old.assert.isEqualTo("Test Glossary")
      modifications["baseLanguageTag"]!!.old.assert.isEqualTo("en")
      latestActivityRevision.organizationId.assert.isEqualTo(testData.organization.id)

      modifiedEntities.filter { it.entityClass == GlossaryTerm::class.simpleName }.assert.hasSize(4)
      modifiedEntities.filter { it.entityClass == GlossaryTermTranslation::class.simpleName }.assert.hasSize(6)

      val glossaryRelation =
        latestActivityRevision.describingRelations.find { it.entityClass == Glossary::class.simpleName }
      glossaryRelation?.data["name"].assert.isEqualTo("Test Glossary")
      glossaryRelation?.data["baseLanguageTag"].assert.isEqualTo("en")

      // we need to have the term relation stored to transitively find the id of the glossary
      latestActivityRevision.describingRelations
        .filter { it.entityClass == GlossaryTerm::class.simpleName }
        .assert
        .hasSize(4)
    }
  }

  @Test
  fun `stores activity on assigned projects update`() {
    val request =
      UpdateGlossaryRequest().apply {
        name = testData.glossary.name
        baseLanguageTag = testData.glossary.baseLanguageTag
        assignedProjectIds = mutableSetOf(testData.anotherProject.id, testData.anotherProject2.id)
      }

    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/assigned-projects")
      .andIsOk

    // Verify activity logging for assigned projects
    executeInNewTransaction {
      val latestActivityRevision = getLatestActivityRevision()
      val modifiedEntities = latestActivityRevision.modifiedEntities
      modifiedEntities.size.assert.isEqualTo(1)

      // Verify activity type
      latestActivityRevision.type.assert.isEqualTo(ActivityType.GLOSSARY_UPDATE)

      val modifications = modifiedEntities.single().modifications
      val assignedProjectsModification = modifications["assignedProjects"]
      assignedProjectsModification.assert.isNotNull()

      // Verify that the old and new values have the expected sizes
      val oldValue = assignedProjectsModification!!.old as List<*>
      oldValue.size.assert.isEqualTo(1)

      val newValue = assignedProjectsModification.new as List<*>
      newValue.size.assert.isEqualTo(2)

      // Verify that the old and new values are different
      (oldValue != newValue).assert.isTrue()

      // Test also that the new value contains the correct project id and name
      // We can see from the debug output that the newValue list contains two maps with "id" and "name" keys
      newValue.assert.anySatisfy { item ->
        item is Map<*, *> && item["name"] == "Another1" && item["id"] == testData.anotherProject.id
      }

      newValue.assert.anySatisfy { item ->
        item is Map<*, *> && item["name"] == "Another2" && item["id"] == testData.anotherProject2.id
      }
    }
  }

  private fun getLatestActivityRevision(): ActivityRevision =
    entityManager
      .createQuery("from ActivityRevision ar order by ar.timestamp desc ", ActivityRevision::class.java)
      .setMaxResults(1)
      .singleResult
}
