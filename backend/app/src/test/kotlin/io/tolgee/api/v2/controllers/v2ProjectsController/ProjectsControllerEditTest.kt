package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerEditTest : AuthorizedControllerTest() {
  @Test
  fun `edits project`() {
    val base = dbPopulator.createBase("What a project")
    val content =
      EditProjectRequest(
        name = "new name",
        baseLanguageId = base.project.languages.toList()[1].id,
        slug = "new-slug",
        icuPlaceholders = true,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andPrettyPrint.andIsOk.andAssertThatJson {
      node("name").isEqualTo(content.name)
      node("slug").isEqualTo(content.slug)
      node("baseLanguage.id").isEqualTo(content.baseLanguageId)
      node("icuPlaceholders").isEqualTo(content.icuPlaceholders)
    }
  }

  @Test
  fun `validates project on edit`() {
    val base = dbPopulator.createBase("What a project")
    val content =
      EditProjectRequest(
        name = "",
        baseLanguageId = base.project.languages.toList()[0].id,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION.name").isNotNull
    }
  }

  @Test
  fun `automatically chooses base language`() {
    val base = dbPopulator.createBase("What a project")
    val content =
      EditProjectRequest(
        name = "test",
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andPrettyPrint.andIsOk.andAssertThatJson {
      node("name").isEqualTo(content.name)
      node("baseLanguage.id").isEqualTo(base.project.languages.toList()[0].id)
    }
  }
}
