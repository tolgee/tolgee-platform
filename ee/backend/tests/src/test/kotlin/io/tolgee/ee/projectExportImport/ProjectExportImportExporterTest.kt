package io.tolgee.ee.projectExportImport

import com.fasterxml.jackson.core.type.TypeReference
import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ProjectExportImportTestData
import io.tolgee.service.AvatarService
import io.tolgee.service.projectExportImport.ProjectExportImportExporter
import io.tolgee.service.projectExportImport.model.ExportManifest
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import io.tolgee.service.projectExportImport.model.SerializedEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Date
import java.util.zip.ZipInputStream

@SpringBootTest
class ProjectExportImportExporterTest : AbstractSpringTest() {
  @Autowired
  private lateinit var exporter: ProjectExportImportExporter

  private lateinit var testData: ProjectExportImportTestData
  private lateinit var zip: Map<String, ByteArray>

  @BeforeEach
  fun setup() {
    testData = ProjectExportImportTestData()
    testDataService.saveTestData(testData.root)
    fileStorage.storeFile(AvatarService.getAvatarPaths(testData.avatarHash).large, AVATAR_BYTES)
    currentDateProvider.forcedDate = Date(EXPORTED_AT)
    zip = exportZip()
  }

  @AfterEach
  fun resetClock() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `manifest carries the passed version, project name and counts`() {
    val manifest = objectMapper.readValue(zip.getValue(ExportZipLayout.MANIFEST), ExportManifest::class.java)
    assertThat(manifest.schemaVersion).isEqualTo(VERSION)
    assertThat(manifest.sourceProjectName).isEqualTo(testData.project.name)
    assertThat(manifest.entityCounts["Screenshot"]).isEqualTo(1)
    assertThat(manifest.entityCounts["Label"]).isEqualTo(2)
    assertThat(manifest.exportedAt).isEqualTo(EXPORTED_AT)
  }

  @Test
  fun `discovers OWNED types unreachable by a graph walk`() {
    val labelNames = entities("Label").map { it.attrs["name"] }
    assertThat(labelNames).contains(testData.assignedLabelName, testData.unassignedLabelName)
    assertThat(entities("ProjectQaConfig")).hasSize(1)
    assertThat(entities("LanguageQaConfig")).hasSize(1)
  }

  @Test
  fun `exports live rows through every multi-hop collector`() {
    assertThat(entities("Namespace").map { it.attrs["name"] }).contains(testData.liveNamespaceName)
    assertThat(entities("Tag").map { it.attrs["name"] }).contains(testData.liveTagName)
    assertThat(entities("KeyMeta").map { it.attrs["description"] }).contains(testData.liveKeyMetaDescription)
    assertThat(entities("KeyCodeReference").map { it.attrs["path"] }).contains(testData.liveCodeReferencePath)
    assertThat(entities("TranslationComment").map { it.attrs["text"] }).contains(testData.commentText)
  }

  @Test
  fun `collects a screenshot shared by two keys exactly once and keeps id-components only in the handle`() {
    assertThat(entities("Screenshot")).hasSize(1)
    val references = entities("KeyScreenshotReference")
    assertThat(references).hasSize(2)
    references.forEach {
      assertThat(it.handle).isInstanceOf(Map::class.java)
      @Suppress("UNCHECKED_CAST")
      val handle = it.handle as Map<String, Any?>
      assertThat(handle.keys).contains("key", "screenshot")
      assertThat((handle["screenshot"] as Number).toLong()).isEqualTo(testData.sharedScreenshot.id)
      assertThat(it.assocs).doesNotContainKeys("key", "screenshot")
    }
  }

  @Test
  fun `exports blobs named by source handle and deduplicates a shared screenshot`() {
    val screenshotBlobs = zip.keys.filter { it.startsWith("${ExportZipLayout.BLOBS_DIR}screenshots/") }
    assertThat(screenshotBlobs).hasSize(1)
    val expectedScreenshotPath = ExportZipLayout.blobPath("screenshots/${testData.sharedScreenshot.id}.jpg")
    assertThat(screenshotBlobs.single()).isEqualTo(expectedScreenshotPath)
    assertThat(zip.getValue(expectedScreenshotPath)).isEqualTo(testData.screenshotImageBytes)

    val avatarPath = ExportZipLayout.blobPath("avatar/${testData.project.id}.png")
    assertThat(zip).containsKey(avatarPath)
    assertThat(zip.getValue(avatarPath)).isEqualTo(AVATAR_BYTES)
  }

  @Test
  fun `serializes a user association as a username handle`() {
    val authorRefs = entities("TranslationComment").mapNotNull { it.assocs["author"] }
    assertThat(authorRefs).isNotEmpty()
    assertThat(authorRefs).anySatisfy { ref ->
      assertThat(ref).isInstanceOf(Map::class.java)
      assertThat((ref as Map<*, *>)["username"]).isEqualTo(testData.user.username)
    }
  }

  @Test
  fun `serializes a to-many owning association as a list of id handles`() {
    val labeled = entities("Translation").single { it.attrs["text"] == "Labeled" }
    val labels = labeled.assocs["labels"]
    assertThat(labels).isInstanceOf(List::class.java)
    assertThat((labels as List<*>).map { (it as Number).toLong() })
      .containsExactly(testData.assignedLabel.id)
  }

  @Test
  fun `drops a nullable association to a soft-deleted target`() {
    assertThat(entities("Branch").map { it.attrs["name"] }).doesNotContain(testData.deletedBranchName)
    val keyOnDeletedBranch = entities("Key").single { it.attrs["name"] == testData.keyOnDeletedBranchName }
    assertThat(keyOnDeletedBranch.assocs["branch"]).isNull()
  }

  @Test
  fun `excludes a row whose required parent is soft-deleted instead of emitting a null FK`() {
    val taskNames = entities("Task").map { it.attrs["name"] }
    assertThat(taskNames).contains(testData.liveTaskName).doesNotContain(testData.taskOnDeletedLanguageName)

    val taskKeys = entities("TaskKey")
    val taskHandles = taskKeys.mapNotNull { (it.assocs["task"] as? Number)?.toLong() }
    assertThat(taskHandles).doesNotContain(testData.taskOnDeletedLanguage.id)
    val liveTaskKey = taskKeys.single { (it.assocs["task"] as? Number)?.toLong() == testData.liveTask.id }
    assertThat(liveTaskKey.assocs["key"]).isNotNull()
  }

  @Test
  fun `omits an owning association to an IGNORED type entirely`() {
    val liveTaskRecord = entities("Task").single { it.attrs["name"] == testData.liveTaskName }
    assertThat(liveTaskRecord.assocs).doesNotContainKey("agency")
    assertThat(liveTaskRecord.assocs).containsKey("branch")
    assertThat(liveTaskRecord.assocs["branch"]).isNull()
  }

  @Test
  fun `suppresses a @DoNotExport column everywhere in the export`() {
    val translations = entities("Translation")
    assertThat(translations.map { it.attrs["text"] }).contains("Hello")
    translations.forEach { assertThat(it.attrs).doesNotContainKey("promptId") }
    val allBytes = zip.values.joinToString("\n") { it.decodeToString() }
    assertThat(allBytes).doesNotContain(testData.distinctivePromptId.toString())
  }

  @Test
  fun `excludes soft-deleted rows and their children`() {
    val keyNames = entities("Key").map { it.attrs["name"] }
    assertThat(keyNames).contains("greeting", "labeled")
    assertThat(keyNames).doesNotContain(testData.softDeletedKeyName)
    assertThat(entities("Translation").map { it.attrs["text"] })
      .doesNotContain(testData.softDeletedTranslationText)
    assertThat(entities("KeyMeta").map { it.attrs["description"] }).doesNotContain(testData.trashedKeyMetaDescription)
    assertThat(entities("KeyCodeReference").map { it.attrs["path"] }).doesNotContain(testData.trashedCodeReferencePath)
    assertThat(entities("TranslationComment").map { it.attrs["text"] })
      .doesNotContain(testData.trashedTranslationCommentText)
  }

  private fun entities(type: String): List<SerializedEntity> {
    val bytes = zip[ExportZipLayout.entityPath(type)] ?: return emptyList()
    return objectMapper.readValue(bytes, object : TypeReference<List<SerializedEntity>>() {})
  }

  private fun exportZip(): Map<String, ByteArray> {
    val tempFile = exporter.exportToTempFile(testData.project.id, VERSION).path
    try {
      return ZipInputStream(ByteArrayInputStream(Files.readAllBytes(tempFile))).use { stream ->
        generateSequence { stream.nextEntry }
          .filterNot { it.isDirectory }
          .associate { it.name to stream.readAllBytes() }
      }
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  companion object {
    private const val VERSION = "9.9.9-export-test"
    private const val EXPORTED_AT = 1_700_000_000_000L
    private val AVATAR_BYTES = "FAKE-AVATAR-BYTES".toByteArray()
  }
}
