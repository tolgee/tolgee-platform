package io.tolgee.controllers

import io.tolgee.component.demoProject.DemoProjectData
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
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
    val dto = SignUpDto(
      name = "Pavel Novak",
      password = "aaaaaaaaa",
      email = "aaaa@aaaa.com",
      organizationName = demoOrganization
    )
    performPost("/api/public/sign_up", dto).andIsOk
    val project = executeInNewTransaction {
      val organization = organizationRepository.findAllByName(demoOrganization).single()
      val project = organization.projects.single()
      assertThat(project.name).isEqualTo("Demo project")
      val keyCount = DemoProjectData.translations["en"]!!.size
      project.keys.assert.hasSize(keyCount)
      val key = project.keys.find { it.name == "add-item-add-button" }!!
      key.keyScreenshotReferences.assert.hasSize(1)
      val screenshot = key.keyScreenshotReferences.single().screenshot
      screenshot.keyScreenshotReferences.assert.hasSize(keyCount)
      key.translations.assert.hasSize(3)
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
}
