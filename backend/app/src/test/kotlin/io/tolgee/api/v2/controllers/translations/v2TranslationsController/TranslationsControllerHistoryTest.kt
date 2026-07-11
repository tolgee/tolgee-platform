package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerHistoryTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var translation: Translation

  lateinit var testUser: UserAccount
  lateinit var testProject: Project
  lateinit var lang: Language
  lateinit var emptyKey: Key

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
            type = ProjectPermissionType.TRANSLATE
            project = testProject
          }
          lang =
            addLanguage {
              name = "Deutsch"
              tag = "de"
            }.self

          addKey {
            name = "yey"
            emptyKey = this
          }.build {
            addTranslation {
              language = lang
              text = "Test text"
              translation = this
            }
          }
          addKey {
            name = "yey2"
          }
        }
      }
    }

    userAccount = testUser
    projectSupplier = { testProject }
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
          node("modifications.text.old").isEqualTo("nt 19")
          node("modifications.text.new").isEqualTo("nt 20")
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
      node("page.totalElements").isEqualTo(20)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not return comment added events`() {
    performProjectAuthPost(
      "/translations/create-comment",
      mapOf("keyId" to emptyKey.id, "languageId" to lang.id, "text" to "Yey!"),
    )

    val translation =
      transactionTemplate.execute {
        translationService.find(emptyKey, lang).get()
      }

    performProjectAuthGet("/translations/${translation!!.id}/history").andPrettyPrint.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  private fun updateTranslation(
    testTranslation: Translation,
    newText: String,
  ) {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        testTranslation.key.name,
        null,
        mutableMapOf(testTranslation.language.tag to newText),
      ),
    ).andIsOk
  }
}
