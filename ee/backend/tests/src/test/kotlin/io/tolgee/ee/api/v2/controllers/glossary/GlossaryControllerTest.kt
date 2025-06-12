package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
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
class GlossaryControllerTest : AuthorizedControllerTest() {
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
  fun `returns all glossaries`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsOk.andAssertThatJson {
        node("_embedded.glossaries").isArray.hasSize(2)
        node("_embedded.glossaries[0].id").isValidId
        inPath("_embedded.glossaries[*].name").isArray.containsExactlyInAnyOrder("Test Glossary", "Empty Glossary")
      }
  }

  @Test
  fun `does not return all glossaries when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsBadRequest
  }

  @Test
  fun `returns single glossary`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Test Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `does not return single glossary when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsBadRequest
  }

  @Test
  fun `creates glossary`() {
    val request = CreateGlossaryRequest().apply {
      name = "New Glossary"
      baseLanguageTag = "en"
      assignedProjectIds = mutableSetOf(testData.project.id)
    }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsOk.andAssertThatJson {
        node("id").isValidId.satisfies({
          performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/$it")
            .andIsOk.andAssertThatJson {
              node("id").isValidId.isEqualTo(it)
              node("name").isEqualTo("New Glossary")
              node("baseLanguageTag").isEqualTo("en")
            }
        })
        node("name").isEqualTo("New Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `does not create glossary when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request = CreateGlossaryRequest().apply {
      name = "New Glossary"
      baseLanguageTag = "en"
      assignedProjectIds = mutableSetOf(testData.project.id)
    }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsBadRequest
  }

  @Test
  fun `updates glossary`() {
    val request = UpdateGlossaryRequest().apply {
      name = "Updated Glossary"
      baseLanguageTag = "de"
    }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Updated Glossary")
        node("baseLanguageTag").isEqualTo("de")
      }

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Updated Glossary")
        node("baseLanguageTag").isEqualTo("de")
      }
  }

  @Test
  fun `updates glossary assigned projects`() {
    val request = UpdateGlossaryRequest().apply {
      name = testData.glossary.name
      baseLanguageTag = testData.glossary.baseLanguageTag
      assignedProjectIds = mutableSetOf(testData.anotherProject.id, testData.anotherProject2.id)
    }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo(testData.glossary.name)
        node("baseLanguageTag").isEqualTo(testData.glossary.baseLanguageTag)
      }

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/assigned-projects")
      .andIsOk.andAssertThatJson {
        node("_embedded.projects").isArray.hasSize(2)
        inPath(
          "_embedded.projects[*].id"
        ).isArray.containsExactlyInAnyOrder(testData.anotherProject.id, testData.anotherProject2.id)
      }
  }

  @Test
  fun `does not update glossary when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request = UpdateGlossaryRequest().apply {
      name = "Updated Glossary"
      baseLanguageTag = "de"
    }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsBadRequest
  }

  @Test
  fun `deletes glossary`() {
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsNotFound
  }

  @Test
  fun `does not delete glossary when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsBadRequest
  }

  @Test
  fun `stores activity on creation`() {
    val request = CreateGlossaryRequest().apply {
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
    }
  }


  @Test
  fun `stores activity on update`() {
    val request = UpdateGlossaryRequest().apply {
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
      latestActivityRevision.describingRelations.filter { it.entityClass == GlossaryTerm::class.simpleName }
        .assert.hasSize(4)

    }
  }

  private fun getLatestActivityRevision(): ActivityRevision =
    entityManager
      .createQuery("from ActivityRevision ar order by ar.timestamp desc ", ActivityRevision::class.java)
      .setMaxResults(1)
      .singleResult

}
