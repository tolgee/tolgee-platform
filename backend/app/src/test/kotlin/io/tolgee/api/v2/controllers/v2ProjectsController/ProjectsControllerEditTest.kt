package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.model.Project
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerEditTest : AuthorizedControllerTest() {
  private var tmTestData: TranslationMemoryTestData? = null

  @AfterEach
  fun cleanupTm() {
    tmTestData?.let { testDataService.cleanTestData(it.root) }
    tmTestData = null
    userAccount = null
  }

  @Test
  fun `edits project`() {
    val base = dbPopulator.createBase()
    val content =
      EditProjectRequest(
        name = "new name",
        baseLanguageId =
          base.project.languages
            .toList()[1]
            .id,
        slug = "new-slug",
        icuPlaceholders = true,
        useNamespaces = true,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andPrettyPrint.andIsOk.andAssertThatJson {
      node("name").isEqualTo(content.name)
      node("slug").isEqualTo(content.slug)
      node("baseLanguage.id").isEqualTo(content.baseLanguageId)
      node("icuPlaceholders").isEqualTo(content.icuPlaceholders)
      node("useNamespaces").isEqualTo(content.useNamespaces)
    }
  }

  @Test
  fun `validates project on edit`() {
    val base = dbPopulator.createBase()
    val content =
      EditProjectRequest(
        name = "",
        baseLanguageId =
          base.project.languages
            .toList()[0]
            .id,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION.name").isNotNull
    }
  }

  @Test
  fun `automatically chooses base language`() {
    val base = dbPopulator.createBase()
    val content =
      EditProjectRequest(
        name = "test",
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andPrettyPrint.andIsOk.andAssertThatJson {
      node("name").isEqualTo(content.name)
      node("baseLanguage.id").isEqualTo(
        base.project.languages
          .toList()[0]
          .id,
      )
    }
  }

  @Test
  fun `fail validation on disabling namespaces when a namespace exists`() {
    val base = dbPopulator.createBase()
    dbPopulator.createNamespace(base.project)
    val content =
      EditProjectRequest(
        name = "test",
        useNamespaces = false,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andPrettyPrint.andIsBadRequest.andAssertThatJson {
      node("CUSTOM_VALIDATION.namespaces_cannot_be_disabled_when_namespace_exists").isNotNull
    }
  }

  @Test
  fun `can use the same slug as deleted project has`() {
    val base = dbPopulator.createBase()

    executeInNewTransaction {
      val deleted = dbPopulator.createBase()
      deleted.project.deletedAt = currentDateProvider.date
      deleted.project.slug = "new-slug-2"
      projectService.save(deleted.project)
    }
    val slugToBeReused = "new-slug-2"
    val content =
      EditProjectRequest(
        name = "new name",
        baseLanguageId =
          base.project.languages
            .toList()[1]
            .id,
        slug = slugToBeReused,
        icuPlaceholders = true,
        useNamespaces = true,
      )
    performAuthPut("/v2/projects/${base.project.id}", content).andIsOk

    assert2ProjectsWithSameSlugExist(slugToBeReused)
  }

  private fun assert2ProjectsWithSameSlugExist(slug: String) {
    executeInNewTransaction {
      entityManager
        .createQuery("from Project where slug = :slug", Project::class.java)
        .setParameter("slug", slug)
        .resultList.assert
        .hasSize(2)
    }
  }

  @Test
  fun `rejects base language change when shared TMs have mismatched source`() {
    val testData = setupTmFixture()
    val project = testData.projectWithTm
    val germanId = testData.germanLanguageWithTm.id

    performAuthPut(
      "/v2/projects/${project.id}",
      EditProjectRequest(
        name = project.name,
        slug = project.slug,
        baseLanguageId = germanId,
      ),
    ).andIsBadRequest.andAssertThatJson {
      // The conflicting TMs are echoed back so the UI can show the user which assignments
      // would have to be unassigned before the rename.
      node("code").isEqualTo("cannot_change_project_base_language_tm_conflict")
    }
  }

  @Test
  fun `accepts base language change with unassign flag when shared TMs have mismatched source`() {
    val testData = setupTmFixture()
    val project = testData.projectWithTm
    val germanId = testData.germanLanguageWithTm.id

    performAuthPut(
      "/v2/projects/${project.id}",
      EditProjectRequest(
        name = project.name,
        slug = project.slug,
        baseLanguageId = germanId,
        unassignConflictingTms = true,
      ),
    ).andIsOk.andAssertThatJson {
      node("baseLanguage.id").isEqualTo(germanId)
    }

    // After the change, only the project's own PROJECT-type TM should remain
    executeInNewTransaction {
      val remaining =
        entityManager
          .createQuery(
            "select tmp from TranslationMemoryProject tmp where tmp.project.id = :projectId",
            io.tolgee.model.translationMemory.TranslationMemoryProject::class.java,
          ).setParameter("projectId", project.id)
          .resultList
      assertThat(remaining).hasSize(1)
      assertThat(remaining.single().translationMemory.type)
        .isEqualTo(io.tolgee.model.translationMemory.TranslationMemoryType.PROJECT)
    }
  }

  private fun setupTmFixture(): TranslationMemoryTestData {
    val data = TranslationMemoryTestData()
    testDataService.saveTestData(data.root)
    userAccount = data.user
    tmTestData = data
    return data
  }
}
