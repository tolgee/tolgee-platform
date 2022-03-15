package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerHistoryTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var translation: Translation

  lateinit var testUser: UserAccount
  lateinit var testProject: Project

  @BeforeEach
  fun setup() {
    testDataService.saveTestData {
      addUserAccount {
        username = "franta"
        name = "Frantisek"
        testUser = this
      }.build buildUser@{
        setAvatar("e2eTestResources/avatars/png_avatar.png")
        addProject {
          name = "Project"
          userAccount = this@buildUser.self
          testProject = this
        }.build {
          addPermission {
            user = this@buildUser.self
            type = Permission.ProjectPermissionType.TRANSLATE
            project = testProject
          }
          val lang = addLanguage {
            name = "Deutsch"
            tag = "de"
          }.self

          addKey {
            name = "yey"
          }.build {
            addTranslation {
              language = lang
              text = "Test text"
              translation = this
            }
          }
        }
      }
    }

    userAccount = testUser
    projectSupplier = { testProject }
  }

  @AfterEach
  fun cleanup() {
    projectService.deleteProject(project.id)
    userAccountService.delete(userAccount!!)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns history`() {
    val testTranslation = translation
    (1..20).forEach {
      updateTranslation(testTranslation, "nt $it")
    }

    performProjectAuthGet("/translations/${testTranslation.id}/history").andPrettyPrint.andAssertThatJson {
      node("_embedded.revisions") {
        isArray.hasSize(20)
        node("[0]") {
          node("text").isEqualTo("nt 20")
          node("state").isEqualTo("TRANSLATED")
          node("auto").isEqualTo("false")
          node("mtProvider").isEqualTo("null")
          node("timestamp").isNumber.isLessThanOrEqualTo((Date().time * 1000).toBigDecimal())
          node("revisionType").isEqualTo("MOD")
          node("author") {
            node("id").isNumber.isGreaterThan(0.toBigDecimal())
            node("username").isEqualTo("franta")
            node("name").isEqualTo("Frantisek")
            node("avatar") {
              node("large").isString.contains(".png")
              node("thumbnail").isString.contains(".png")
            }
          }
        }
      }
      node("page.totalElements").isEqualTo(21)
    }
  }

  private fun updateTranslation(testTranslation: Translation, newText: String) {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        testTranslation.key.name, mutableMapOf(testTranslation.language.tag to newText)
      )
    ).andIsOk
  }
}
