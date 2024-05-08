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
class DemoProjectTest :
  AbstractControllerTest() {
  @Test
  fun `creates demo project`() {
    val demoOrganization = "Oh my organization"
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        organizationName = demoOrganization,
      )
    performPost("/api/public/sign_up", dto).andIsOk
    val project =
      executeInNewTransaction {
        val organization = organizationRepository.findAllByName(demoOrganization).single()
        val project = organization.projects.single()

        assertProjectName(project)
        assertKeysImported(project)
        assertScreenshotImported(project)
        assertTagged(project)

        project
      }

    waitForNotThrowing {
      executeInNewTransaction {
        val stats = languageStatsService.getLanguageStats(project.id)
        stats.forEach {
          it.reviewedWords.assert.isGreaterThan(0)
        }
      }
    }
  }

  private fun getAddButtonKey(project: Project) = project.keys.find { it.name == "add-item-add-button" }!!

  private fun assertKeysImported(project: Project) {
    project.keys.assert.hasSize(keyCount)
  }

  private fun assertProjectName(project: Project) {
    assertThat(project.name).isEqualTo("Demo project")
  }

  private fun assertScreenshotImported(project: Project) {
    val key = getAddButtonKey(project)
    key.keyScreenshotReferences.assert.hasSize(1)
    val screenshot = key.keyScreenshotReferences.single().screenshot
    screenshot.keyScreenshotReferences.assert.hasSize(keyCount)
    key.translations.assert.hasSize(3)
  }

  private fun assertTagged(project: Project) {
    val key = getAddButtonKey(project)
    key.keyMeta?.tags?.map { it.name }?.contains("button")
  }

  val keyCount = DemoProjectData.translations["en"]!!.size
}
