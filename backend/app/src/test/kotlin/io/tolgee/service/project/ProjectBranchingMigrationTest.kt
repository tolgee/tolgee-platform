package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchingMigrationTestData
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectBranchingMigrationTest : AbstractSpringTest() {
  @Test
  fun `enabling branching migrates data to default branch`() {
    val prepared = prepareTestData()

    executeInNewTransaction {
      val dto =
        EditProjectRequest(
          name = prepared.testData.project.name,
          slug = prepared.testData.project.slug,
          baseLanguageId =
            prepared.testData.project.baseLanguage
              ?.id,
          useNamespaces = prepared.testData.project.useNamespaces,
          useBranching = true,
          defaultNamespaceId =
            prepared.testData.project.defaultNamespace
              ?.id,
          description = prepared.testData.project.description,
          icuPlaceholders = prepared.testData.project.icuPlaceholders,
          suggestionsMode = prepared.testData.project.suggestionsMode,
          translationProtection = prepared.testData.project.translationProtection,
        )

      projectService.editProject(prepared.testData.project.id, dto)
      entityManager.flush()
      entityManager.clear()
    }

    entityManager.clear()

    val refreshedProject = projectRepository.findWithBranches(prepared.testData.project.id)!!
    val defaultBranch = refreshedProject.getDefaultBranch()
    assertThat(defaultBranch).isNotNull()

    val defaultBranchId = defaultBranch!!.id

    val keyBranchId: Long? =
      entityManager
        .createQuery(
          "select k.branch.id from Key k where k.id = :id",
          Long::class.java,
        ).setParameter("id", prepared.testData.key.id)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
    assertThat(keyBranchId).isEqualTo(defaultBranchId)

    val taskBranchId: Long? =
      entityManager
        .createQuery(
          "select t.branch.id from Task t where t.id = :id",
          Long::class.java,
        ).setParameter("id", prepared.testData.task.id)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
    assertThat(taskBranchId).isEqualTo(defaultBranchId)

    val importBranchId: Long? =
      entityManager
        .createQuery(
          "select i.branch.id from Import i where i.id = :id",
          Long::class.java,
        ).setParameter("id", prepared.testData.importEntity.id)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
    assertThat(importBranchId).isEqualTo(defaultBranchId)

    // has any language stats for default branch
    val languageStatsBranchId: Long? =
      entityManager
        .createQuery(
          "select ls.branch.id from LanguageStats ls where ls.branch.id = :defaultBranchId",
          Long::class.java,
        ).setParameter("defaultBranchId", defaultBranchId)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
    assertThat(languageStatsBranchId).isEqualTo(defaultBranchId)
  }

  private fun prepareTestData(): PreparedData {
    val testData = BranchingMigrationTestData()
    testDataService.saveTestData(testData.root)

    val prepared = createBranchlessData(testData)
    entityManager.clear()
    return prepared
  }

  private fun createBranchlessData(testData: BranchingMigrationTestData): PreparedData {
    return executeInNewTransaction {
      entityManager.flush()
      PreparedData(
        testData = testData,
      )
    }
  }

  private data class PreparedData(
    val testData: BranchingMigrationTestData,
  )
}
