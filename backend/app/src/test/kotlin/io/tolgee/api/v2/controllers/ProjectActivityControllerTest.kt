package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import net.javacrumbs.jsonunit.assertj.JsonAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectActivityControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ImportTestData

  @BeforeEach
  fun setup() {
    testData = ImportTestData()
    testData.addManyTranslations()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userAccount
    this.projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns single activity`() {
    performProjectAuthPut("/import/apply").andIsOk
    val revision = getLastRevision()
    performProjectAuthGet("activity/revisions/${revision?.id}")
      .andIsOk.andAssertThatJson {
        assertCountsOnlyResult()
      }
  }

  private fun JsonAssert.assertCountsOnlyResult() {
    node("modifiedEntities").isNull()
    node("counts") {
      node("Key").isEqualTo(1)
      node("KeyMeta").isEqualTo(1)
      node("Translation").isEqualTo(305)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `large operation data only contain counts`() {
    performProjectAuthPut("/import/apply").andIsOk
    performProjectAuthGet("activity")
      .andIsOk.andAssertThatJson {
        node("_embedded.activities") {
          isArray.hasSize(1)
          node("[0]") {
            assertCountsOnlyResult()
          }
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns modified entities with pagination`() {
    performProjectAuthPut("/import/apply").andIsOk
    val revision = getLastRevision()
    performProjectAuthGet("activity/revisions/${revision?.id}/modified-entities")
      .andIsOk.andAssertThatJson {
        node("_embedded.modifiedEntities") {
          isArray
          node("[1]") {
            node("entityId").isValidId
          }
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filters modified entities by class`() {
    performProjectAuthPut("/import/apply").andIsOk
    val revision = getLastRevision()
    performProjectAuthGet("activity/revisions/${revision?.id}/modified-entities?filterEntityClass=Key")
      .andIsOk.andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
      }
  }

  fun getLastRevision(): ActivityRevision? {
    return entityManager.createQuery(
      """
        from ActivityRevision ar order by ar.id desc limit 1
      """.trimMargin(),
      ActivityRevision::class.java,
    ).singleResult
  }
}
