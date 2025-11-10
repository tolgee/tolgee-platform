package io.tolgee.controllers

import io.tolgee.component.demoProject.DemoProjectData
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

@AutoConfigureMockMvc
class DemoProjectTest : AbstractControllerTest() {
  val demoOrganizationName = "Oh my organization"

  @Test
  fun `creates demo project`() {
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        organizationName = demoOrganizationName,
      )
    performPost("/api/public/sign_up", dto).andIsOk
    executeInNewTransaction {
      assertProjectName()
      assertKeysImported()
      assertScreenshotImported()
      assertTagged()
      assertPlural()
      assertDescriptionAdded()
      assertTranslationHasComment()
    }

    assertStatsCreated()
  }

  private fun assertTranslationHasComment() {
    project.keys
      .single { it.name == "app-title" }
      .translations
      .single { it.language.tag == "de" }
      .comments
      .single()
      .text.assert
      .isEqualTo("This is wrong!")
  }

  private fun assertStatsCreated() {
    waitForNotThrowing {
      executeInNewTransaction {
        val project = project
        val stats = languageStatsService.getLanguageStats(project.id)
        stats.forEach {
          it.reviewedWords.assert.isGreaterThan(0)
        }
      }
    }
  }

  private val project: Project
    get() {
      val organization = organizationRepository.findAllByName(demoOrganizationName).single()
      val project = organization.projects.single()
      return project
    }

  private fun assertPlural() {
    val key = getPluralKey()
    key.isPlural.assert.isTrue()
    key.pluralArgName.assert.isEqualTo("count")
  }

  private fun getAddButtonKey() = project.keys.find { it.name == "add-item-add-button" }!!

  private fun getPluralKey() = project.keys.find { it.name == "items-count" }!!

  private fun assertKeysImported() {
    project.keys.assert.hasSize(keyCount)
    getAddButtonKey().translations.assert.hasSize(3)
  }

  private fun assertProjectName() {
    assertThat(project.name).isEqualTo("Demo project")
  }

  private fun assertScreenshotImported() {
    val key = getAddButtonKey()
    key.keyScreenshotReferences.assert.hasSize(1)
    val screenshot = key.keyScreenshotReferences.single().screenshot
    screenshot.keyScreenshotReferences.assert.hasSize(7)
  }

  private fun assertTagged() {
    val key = getAddButtonKey()
    key.keyMeta
      ?.tags
      ?.map { it.name }
      ?.contains("button")
  }

  fun assertDescriptionAdded() {
    val key = getAddButtonKey()
    key.keyMeta!!
      .description.assert
      .isNotNull()
      .isNotEmpty()
  }

  val keyCount = DemoProjectData.translations["en"]!!.size
}
