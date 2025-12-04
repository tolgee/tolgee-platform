package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportBranchTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.util.performImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional

@Transactional
class V2ImportControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  lateinit var testData: ImportBranchTestData

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
}
