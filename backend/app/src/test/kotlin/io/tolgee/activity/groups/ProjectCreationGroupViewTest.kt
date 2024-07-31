package io.tolgee.activity.groups

import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsJsonMap
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.key.Key
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class ProjectCreationGroupViewTest : AuthorizedControllerTest() {
  private lateinit var testData: BaseTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  lateinit var key: Key

  private fun prepareTestData() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `shows correct project creation group`() {
    prepareTestData()
    userAccount = testData.user
    val projectId = performProjectCreation()
    performAuthGet("/v2/projects/$projectId/activity/groups")
      .andIsOk.andAssertThatJson {
        node("_embedded.groups") {
          isArray.hasSizeGreaterThan(0)
          node("[0]") {
            node("id").isNumber
            node("timestamp").isNumber.isGreaterThan(BigDecimal("1722441564241"))
            node("type").isEqualTo("CREATE_PROJECT")

            node("author") {
              node("id").isNumber
              node("username").isEqualTo("test_username")
              node("name").isEqualTo("")
              node("avatar").isNull()
              node("deleted").isEqualTo(false)
            }

            node("counts.Project").isEqualTo(1)

            node("data") {
              node("id").isNumber
              node("name").isEqualTo("What a project")
              node("languages") {
                isArray.hasSize(2)
                node("[0]") {
                  node("id").isValidId
                  node("name").isEqualTo("English")
                  node("originalName").isEqualTo("English")
                  node("tag").isEqualTo("en")
                  node("flagEmoji").isEqualTo("a")
                }
                node("[1]") {
                  node("id").isValidId
                  node("name").isEqualTo("Czech")
                  node("originalName").isEqualTo("česky")
                  node("tag").isEqualTo("cs")
                  node("flagEmoji").isEqualTo("b")
                }
              }
              node("description").isNull()
            }
          }
        }
      }
  }

  private fun performProjectCreation(): Int {
    return performAuthPost(
      "/v2/projects",
      CreateProjectRequest(
        name = "What a project",
        organizationId = testData.project.organizationOwner.id,
        languages =
          listOf(
            LanguageRequest(
              name = "English",
              originalName = "English",
              tag = "en",
              flagEmoji = "a",
            ),
            LanguageRequest(
              name = "Czech",
              originalName = "česky",
              tag = "cs",
              flagEmoji = "b",
            ),
          ),
        baseLanguageTag = "cs",
      ),
    ).andIsOk.andGetContentAsJsonMap["id"] as Int
  }
}
