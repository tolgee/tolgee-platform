package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.ProjectExportImportTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.service.projectExportImport.ProjectExportImportExporter
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.VersionProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
class ProjectExportImportControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: ProjectExportImportTestData

  @Autowired
  private lateinit var exporter: ProjectExportImportExporter

  @Autowired
  private lateinit var versionProvider: VersionProvider

  private val pngSignature =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

  @BeforeEach
  fun setup() {
    testData = ProjectExportImportTestData(projectName = "weird/name*?")
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `export is forbidden without super auth`() {
    userAccount = testData.user
    performAuthGet("/v2/administration/projects/${testData.project.id}/export").andIsForbidden
  }

  @Test
  fun `export returns a project zip for a super admin`() {
    userAccount = testData.adminUser
    // MockMvc can't reliably drain the StreamingResponseBody (async dispatch closes the Hibernate session),
    // so assert only status + headers here; the payload is covered by the exporter and round-trip tests.
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
      ZipInputStream(ByteArrayInputStream(exportedZipBytes())).use { stream ->
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

    // A stray key present only in the target (absent from the export) must be gone after the mirror
    // import — otherwise a no-op import would also satisfy the "content is present" assertion.
    addKey("stray-should-be-wiped")

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
    ).andIsOk

    assertThat(projectKeyNames())
      .contains("greeting", "labeled")
      .doesNotContain("stray-should-be-wiped")
  }

  @Test
  fun `import restores BigMeta and the default project TM end-to-end`() {
    val zip = exportedZipBytes()

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
    ).andIsOk

    executeInNewTransaction {
      val bigMetaCount =
        entityManager
          .createQuery(
            "select count(kd) from KeysDistance kd where kd.project.id = :p",
            java.lang.Long::class.java,
          ).setParameter("p", testData.project.id)
          .singleResult
          .toLong()
      assertThat(bigMetaCount).isEqualTo(1)

      val projectTmCount =
        entityManager
          .createQuery(
            "select count(a) from TranslationMemoryProject a " +
              "where a.project.id = :p and a.translationMemory.type = :t",
            java.lang.Long::class.java,
          ).setParameter("p", testData.project.id)
          .setParameter("t", TranslationMemoryType.PROJECT)
          .singleResult
          .toLong()
      assertThat(projectTmCount).isEqualTo(1)
    }
  }

  @Test
  fun `import rejects a version mismatch without the override`() {
    val zip = tamperManifestVersion(exportedZipBytes(), "some-other-version")

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
    ).andIsBadRequest
  }

  @Test
  fun `import bypasses a version mismatch with ignoreVersion`() {
    val zip = tamperManifestVersion(exportedZipBytes(), "some-other-version")

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
      mapOf("ignoreVersion" to arrayOf("true")),
    ).andIsOk

    assertThat(projectKeyNames()).contains("greeting", "labeled")
  }

  // Headerless bytes make ImageIO.read return null; a valid PNG header over a corrupt body makes it throw
  // IIOException. Both decode-failure paths must surface as a clean 400, not a 500.
  @Test
  fun `import rejects an archive whose screenshot blob has no image header`() {
    assertScreenshotBlobRejected("not a real image".toByteArray())
  }

  @Test
  fun `import rejects an archive whose screenshot blob has a valid header but corrupt body`() {
    assertScreenshotBlobRejected(pngSignature + "corrupt-png-body".toByteArray())
  }

  // A 6000x1 image decodes fine but its scaled thumbnail height floors to 0, so ImageConverter.getThumbnail
  // throws — a decodable-but-unthumbnailable blob that must still be a clean 400, not a 500.
  @Test
  fun `import rejects an archive whose screenshot blob is a decodable but non-thumbnailable image`() {
    val degenerate =
      ByteArrayOutputStream().use { out ->
        ImageIO.write(BufferedImage(6000, 1, BufferedImage.TYPE_INT_RGB), "png", out)
        out.toByteArray()
      }
    assertScreenshotBlobRejected(degenerate)
  }

  private fun assertScreenshotBlobRejected(corruptBlob: ByteArray) {
    val zip =
      rewriteZipEntries(exportedZipBytes()) { entryName, bytes ->
        if (!entryName.startsWith("blobs/screenshots/")) return@rewriteZipEntries bytes
        corruptBlob
      }

    userAccount = testData.adminUser
    performAuthMultipart(
      "/v2/administration/projects/${testData.project.id}/import",
      listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
    ).andIsBadRequest
  }

  @Test
  fun `a failed override import rolls back, leaving the project intact`() {
    val zip =
      replaceEntry(
        tamperManifestVersion(exportedZipBytes(), "some-other-version"),
        "entities/Key.json",
        "{ not valid json".toByteArray(),
      )

    userAccount = testData.adminUser
    val status =
      performAuthMultipart(
        "/v2/administration/projects/${testData.project.id}/import",
        listOf(MockMultipartFile("file", "export.zip", "application/zip", zip)),
        mapOf("ignoreVersion" to arrayOf("true")),
      ).andReturn().response.status
    assertThat(status).isGreaterThanOrEqualTo(400)

    assertThat(projectKeyNames()).contains("greeting", "labeled")
  }

  private fun replaceEntry(
    zip: ByteArray,
    name: String,
    newBytes: ByteArray,
  ): ByteArray =
    rewriteZipEntries(zip) { entryName, bytes ->
      if (entryName != name) return@rewriteZipEntries bytes
      newBytes
    }

  private fun tamperManifestVersion(
    zip: ByteArray,
    newVersion: String,
  ): ByteArray =
    rewriteZipEntries(zip) { entryName, bytes ->
      if (entryName != "manifest.json") return@rewriteZipEntries bytes
      String(bytes)
        .replace(Regex("\"schemaVersion\"\\s*:\\s*\"[^\"]*\""), "\"schemaVersion\":\"$newVersion\"")
        .toByteArray()
    }

  private fun rewriteZipEntries(
    zip: ByteArray,
    transform: (name: String, bytes: ByteArray) -> ByteArray,
  ): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zos ->
      ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
        generateSequence { zis.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
          val bytes = transform(entry.name, zis.readAllBytes())
          zos.putNextEntry(ZipEntry(entry.name))
          zos.write(bytes)
          zos.closeEntry()
        }
      }
    }
    return out.toByteArray()
  }

  // Export via the service, not the HTTP endpoint: MockMvc doesn't deterministically drain a
  // StreamingResponseBody, which would make every import test flaky.
  private fun exportedZipBytes(): ByteArray {
    val export = exporter.exportToTempFile(testData.project.id, versionProvider.version)
    return try {
      Files.readAllBytes(export.path)
    } finally {
      Files.deleteIfExists(export.path)
    }
  }

  private fun addKey(name: String) {
    userAccount = testData.user
    performAuthPost("/v2/projects/${testData.project.id}/keys", CreateKeyDto(name = name)).andIsCreated
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
