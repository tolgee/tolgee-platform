package io.tolgee.ee.api.v2.controllers.branching

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.dataImport.ImportBranchTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.util.performImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Transactional
class V2ImportControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  lateinit var testData: ImportBranchTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = ImportBranchTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `add files returns result only for selected branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      mvc = mvc,
      projectId = testData.project.id,
      files = listOf("simple.json" to simpleJson),
      params = mapOf("branch" to testData.featureBranch.name),
    ).andIsOk.andAssertThatJson {
      node("result._embedded.languages").isArray.hasSize(1)
    }

    performProjectAuthGet("import/result?branch=${testData.featureBranch.name}")
      .andIsOk
      .andAssertThatJson { node("_embedded.languages").isArray.hasSize(1) }

    performProjectAuthGet("import/result")
      .andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancel removes only branch import`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      mvc = mvc,
      projectId = testData.project.id,
      files = listOf("simple.json" to simpleJson),
    )

    performImport(
      mvc = mvc,
      projectId = testData.project.id,
      files = listOf("simple.json" to simpleJson),
      params = mapOf("branch" to testData.featureBranch.name),
    )

    performProjectAuthDelete("import?branch=${testData.featureBranch.name}")
      .andIsOk

    performProjectAuthGet("import/result?branch=${testData.featureBranch.name}")
      .andIsNotFound

    performProjectAuthGet("import/result")
      .andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_EDIT, Scope.KEYS_CREATE])
  fun `apply into protected branch is forbidden without protected-modify scope`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      mvc = mvc,
      projectId = testData.project.id,
      files = listOf("simple.json" to simpleJson),
      params = mapOf("branch" to testData.protectedBranch.name),
    ).andIsOk
    performProjectAuthPut("import/apply?branch=${testData.protectedBranch.name}", null).andIsForbidden
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_EDIT, Scope.KEYS_CREATE, Scope.BRANCH_PROTECTED_MODIFY],
  )
  fun `apply into protected branch passes with protected-modify scope`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    val importResponse =
      performImport(
        mvc = mvc,
        projectId = testData.project.id,
        files = listOf("simple.json" to simpleJson),
        params = mapOf("branch" to testData.protectedBranch.name),
      ).andIsOk.andReturn().response.contentAsString
    val importLanguageId =
      ObjectMapper()
        .readTree(
          importResponse,
        ).at("/result/_embedded/languages/0/id")
        .asLong()
    performProjectAuthPut(
      "import/result/languages/$importLanguageId/select-existing/${testData.englishLanguage.id}",
      null,
    ).andIsOk
    performProjectAuthPut("import/apply?branch=${testData.protectedBranch.name}", null).andIsOk
  }
}
