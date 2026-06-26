package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.ProjectExportImportTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@SpringBootTest
@AutoConfigureMockMvc
class ProjectExportImportControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: ProjectExportImportTestData

  @BeforeEach
  fun setup() {
    testData = ProjectExportImportTestData(projectName = "weird/name*?")
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `export is forbidden without super auth`() {
    userAccount = testData.user
    performAuthGet("/v2/administration/projects/${testData.project.id}/export").andIsForbidden
  }

  @Test
  fun `export returns a project zip for a super admin`() {
    userAccount = testData.adminUser
    val response =
      performAuthGet("/v2/administration/projects/${testData.project.id}/export")
        .andIsOk
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
        .response
    assertThat(response.getHeaderValue("content-type").toString()).contains("application/zip")
    val contentDisposition = response.getHeaderValue("content-disposition").toString()
    assertThat(contentDisposition).contains("attachment").contains("weird_name__.zip").doesNotContain("/")

    val entryNames =
      ZipInputStream(ByteArrayInputStream(response.contentAsByteArray)).use { stream ->
        generateSequence { stream.nextEntry }.map { it.name }.toList()
      }
    assertThat(entryNames).contains("manifest.json")
  }

  @Test
  fun `import is forbidden without super auth`() {
    userAccount = testData.user
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", byteArrayOf(1, 2, 3))),
    ).andIsForbidden
  }

  @Test
  fun `import wipes and replaces the project for a super admin`() {
    val zip = exportedZipBytes()

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
    ).andIsOk

    assertThat(projectKeyNames()).contains("greeting", "labeled")
  }

  private fun exportedZipBytes(): ByteArray {
    userAccount = testData.adminUser
    return performAuthGet("/v2/administration/projects/${testData.project.id}/export")
      .andIsOk
      .andDo { obj: MvcResult -> obj.asyncResult }
      .andReturn()
      .response.contentAsByteArray
  }

  // The import commits in its own transaction; read it back in a fresh one so the assertion sees it.
  private fun projectKeyNames(): List<String> =
    executeInNewTransaction {
      entityManager
        .createQuery("select k.name from Key k where k.project.id = :p", String::class.java)
        .setParameter("p", testData.project.id)
        .resultList
    }
}
