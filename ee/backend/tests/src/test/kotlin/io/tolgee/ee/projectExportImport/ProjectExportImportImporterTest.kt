package io.tolgee.ee.projectExportImport

import com.fasterxml.jackson.core.type.TypeReference
import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ProjectExportImportTestData
import io.tolgee.development.testDataBuilder.data.ProjectImportBranchedSourceTestData
import io.tolgee.development.testDataBuilder.data.ProjectImportTargetTestData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Screenshot
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.service.AvatarService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.projectExportImport.ProjectExportImportExporter
import io.tolgee.service.projectExportImport.ProjectExportImportImporter
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import io.tolgee.service.projectExportImport.model.SerializedEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@SpringBootTest
class ProjectExportImportImporterTest : AbstractSpringTest() {
  @Autowired
  private lateinit var exporter: ProjectExportImportExporter

  @Autowired
  private lateinit var importer: ProjectExportImportImporter

  private lateinit var source: ProjectExportImportTestData
  private lateinit var target: ProjectImportTargetTestData

  @BeforeEach
  fun setup() {
    source = ProjectExportImportTestData(projectName = "import-source-project")
    testDataService.saveTestData(source.root)
    fileStorage.storeFile(AvatarService.getAvatarPaths(source.avatarHash).large, AVATAR_BYTES)

    target = ProjectImportTargetTestData()
    testDataService.saveTestData(target.root)
  }

  @Test
  fun `mirrors every OWNED column onto the target (reflective parity round-trip)`() {
    importSourceOntoTarget()

    val sourceZip = exportZip(source.project.id)
    val targetZip = exportZip(target.targetProject.id)

    ownedTypeNames(sourceZip).forEach { type ->
      assertThat(comparableAttrs(targetZip, type))
        .withFailMessage("OWNED type %s did not round-trip identically (column fidelity)", type)
        .containsExactlyInAnyOrderElementsOf(comparableAttrs(sourceZip, type))
    }
  }

  @Test
  fun `the parity comparison can detect a single mutated column (negative control)`() {
    val sourceZip = exportZip(source.project.id)
    val original = comparableAttrs(sourceZip, "Key")
    val mutated = original.first().toMutableMap().apply { this["name"] = "MUTATED" }
    assertThat(original).doesNotContain(mutated)
  }

  @Test
  fun `replaces the target's content with exactly the source's and leaves the sibling untouched`() {
    importSourceOntoTarget()

    assertThat(keyNames(target.targetProject.id))
      .containsExactlyInAnyOrder(
        "greeting",
        "labeled",
        "rich-key",
        "screenshot-key-1",
        "screenshot-key-2",
        source.keyOnDeletedBranchName,
        source.keyForExcludedTaskName,
        source.keyForLiveTaskName,
        source.suggestionKeyName,
        source.qaKeyName,
      )
    assertThat(labelNames(target.targetProject.id))
      .containsExactlyInAnyOrder(source.assignedLabelName, source.unassignedLabelName)

    assertThat(keyNames(target.siblingProject.id)).containsExactly(target.siblingKeyName)
    assertThat(labelNames(target.siblingProject.id)).containsExactly(target.siblingLabelName)
  }

  @Test
  fun `re-wires associations across the reassigned primary keys`() {
    importSourceOntoTarget()
    val projectId = target.targetProject.id

    assertThat(labelNamesOnTranslation(projectId, "Labeled")).containsExactly(source.assignedLabelName)
    assertThat(tagNamesOnKey(projectId, "rich-key")).contains(source.liveTagName)
    assertThat(taskKeyCount(projectId, source.liveTaskName)).isEqualTo(1)
    // A screenshot shared by two source keys must re-materialize as one Screenshot with two references,
    // not be duplicated per key.
    assertThat(screenshotCount(projectId)).isEqualTo(1)
    assertThat(keyScreenshotReferenceCount(projectId)).isEqualTo(2)
  }

  @Test
  fun `excludes soft-deleted source content from the import`() {
    importSourceOntoTarget()
    val projectId = target.targetProject.id

    assertThat(keyNames(projectId)).doesNotContain(source.softDeletedKeyName)
    assertThat(translationTexts(projectId)).doesNotContain(source.softDeletedTranslationText)
    assertThat(branchNames(projectId)).doesNotContain(source.deletedBranchName)
    assertThat(languageTags(projectId)).doesNotContain("zz-deleted")
  }

  @Test
  fun `clears the target avatar when the source has none`() {
    // Give the target a starting avatar so the mirror-clear branch is actually exercised (not vacuously null).
    executeInNewTransaction {
      entityManager
        .createQuery("update Project p set p.avatarHash = 'pre-existing-target-avatar' where p.id = :id")
        .setParameter("id", target.targetProject.id)
        .executeUpdate()
    }
    assertThat(projectAvatarHash(target.targetProject.id)).isEqualTo("pre-existing-target-avatar")

    val zip = exportZip(source.project.id)
    val withoutAvatar = rezipWithout(zip) { it.startsWith("${ExportZipLayout.BLOBS_DIR}avatar/") }
    importer.import(ByteArrayInputStream(withoutAvatar), target.targetProject.id, source.adminUser.id, VERSION)

    assertThat(projectAvatarHash(target.targetProject.id)).isNull()
  }

  @Test
  fun `wipes a target branch carrying merge and snapshot rows without an FK violation`() {
    // The target carries branch_merge / branch_merge_change / branch_key_snapshot rows (see the test data);
    // they FK key/branch with no DB cascade, so the clear must delete them before keys/branches.
    importSourceOntoTarget()

    assertThat(branchCount(target.targetProject.id)).isZero()
    assertThat(nativeCount("branch_merge", "source_branch_id", target.targetBranch.id)).isZero()
    assertThat(nativeCount("branch_merge_change", "source_key_id", target.targetOldKey.id)).isZero()
    assertThat(nativeCount("branch_key_snapshot", "project_id", target.targetProject.id)).isZero()
  }

  @Test
  fun `preserves a matched author rather than attributing to the admin`() {
    importSourceOntoTarget()
    assertThat(commentAuthorUsername(target.targetProject.id, source.commentText))
      .isEqualTo(source.user.username)
  }

  @Test
  fun `falls back to the importing admin for an author absent on this instance`() {
    executeInNewTransaction {
      entityManager
        .createQuery("update UserAccount u set u.deletedAt = CURRENT_TIMESTAMP where u.id = :id")
        .setParameter("id", source.user.id)
        .executeUpdate()
    }

    importSourceOntoTarget()

    assertThat(commentAuthorUsername(target.targetProject.id, source.commentText))
      .isEqualTo(source.adminUser.username)
  }

  @Test
  fun `imports a suggestion with its key, language and matched author`() {
    importSourceOntoTarget()
    val suggestion = singleSuggestion(target.targetProject.id, source.suggestionText)
    assertThat(suggestion.key.name).isEqualTo(source.suggestionKeyName)
    assertThat(suggestion.language?.tag).isEqualTo("en")
    assertThat(suggestion.author?.username).isEqualTo(source.suggestionAuthor.username)
    assertThat(suggestion.isPlural).isTrue()
    assertThat(suggestion.state).isEqualTo(TranslationSuggestionState.DECLINED)
  }

  @Test
  fun `falls back to the importing admin for a suggestion author absent on this instance`() {
    executeInNewTransaction {
      entityManager
        .createQuery("update UserAccount u set u.deletedAt = CURRENT_TIMESTAMP where u.id = :id")
        .setParameter("id", source.suggestionAuthor.id)
        .executeUpdate()
    }

    importSourceOntoTarget()

    assertThat(singleSuggestion(target.targetProject.id, source.suggestionText).author?.username)
      .isEqualTo(source.adminUser.username)
  }

  @Test
  fun `imports QA issues on the right translation and preserves the dismissal state`() {
    importSourceOntoTarget()
    val issues = qaIssues(target.targetProject.id, source.qaKeyName, "en")

    val open = issues.single { it.replacement == source.qaIssueReplacement }
    assertThat(open.state).isEqualTo(QaIssueState.OPEN)
    assertThat(open.type).isEqualTo(QaCheckType.PUNCTUATION_MISMATCH)
    assertThat(open.message).isEqualTo(QaIssueMessage.QA_PUNCTUATION_ADD)
    assertThat(open.params).isEqualTo(source.qaIssueParams)
    assertThat(open.positionStart).isEqualTo(source.qaOpenPositionStart)
    assertThat(open.positionEnd).isEqualTo(source.qaOpenPositionEnd)
    assertThat(open.virtual).isTrue()
    assertThat(open.pluralVariant).isEqualTo(source.qaIssuePluralVariant)

    val dismissed = issues.single { it.replacement == null }
    assertThat(dismissed.state).isEqualTo(QaIssueState.IGNORED)
    assertThat(dismissed.type).isEqualTo(QaCheckType.EMPTY_TRANSLATION)
  }

  @Test
  fun `preserves the qaChecksStale flag across the import`() {
    importSourceOntoTarget()
    val projectId = target.targetProject.id
    assertThat(qaChecksStale(projectId, source.staleTrueKeyName)).isTrue()
    assertThat(qaChecksStale(projectId, source.qaKeyName)).isFalse()
  }

  @Test
  fun `clears the target's pre-existing suggestions and QA issues`() {
    importSourceOntoTarget()
    val projectId = target.targetProject.id

    assertThat(suggestionTexts(projectId))
      .contains(source.suggestionText)
      .doesNotContain(target.oldTargetSuggestionText)
    assertThat(qaIssueReplacements(projectId))
      .contains(source.qaIssueReplacement)
      .doesNotContain(target.oldTargetQaReplacement)
  }

  @Test
  fun `aborts on a version mismatch with the target untouched`() {
    val zip = exportZip(source.project.id)
    assertThatThrownBy {
      importer.import(ByteArrayInputStream(zip), target.targetProject.id, source.adminUser.id, "wrong-version")
    }.isInstanceOf(BadRequestException::class.java)

    assertThat(keyNames(target.targetProject.id)).contains(target.oldKeyName)
  }

  @Test
  fun `with ignoreVersion, replaces content despite a version-string mismatch`() {
    val zip = exportZip(source.project.id)
    importer.import(
      ByteArrayInputStream(zip),
      target.targetProject.id,
      source.adminUser.id,
      "wrong-version",
      ignoreVersion = true,
    )

    assertThat(keyNames(target.targetProject.id))
      .contains("greeting")
      .doesNotContain(target.oldKeyName)
  }

  @Test
  fun `produces no activity revisions`() {
    val before = activityRevisionCount()
    importSourceOntoTarget()
    assertThat(activityRevisionCount()).isEqualTo(before)
  }

  @Test
  fun `round-trips screenshot bytes and gives the avatar a fresh non-dangling hash`() {
    importSourceOntoTarget()

    val screenshot = singleScreenshot(target.targetProject.id)
    assertThat(fileStorage.readFile(screenshotPathFromModel(screenshot))).isEqualTo(source.screenshotImageBytes)

    val avatarHash = projectAvatarHash(target.targetProject.id)
    assertThat(avatarHash).isNotNull().isNotEqualTo(source.avatarHash)
    assertThat(fileStorage.fileExists(AvatarService.getAvatarPaths(avatarHash!!).large)).isTrue()
  }

  @Test
  fun `imports a screenshot row even when its blob is absent from the zip (warn-and-continue)`() {
    val zip = exportZip(source.project.id)
    val withoutScreenshotBlob = rezipWithout(zip) { it.startsWith("${ExportZipLayout.BLOBS_DIR}screenshots/") }
    importer.import(ByteArrayInputStream(withoutScreenshotBlob), target.targetProject.id, source.adminUser.id, VERSION)

    val screenshot = singleScreenshot(target.targetProject.id)
    assertThat(fileStorage.fileExists(screenshotPathFromModel(screenshot))).isFalse()
  }

  @Test
  fun `resets a @DoNotExport column to its default on import`() {
    importSourceOntoTarget()
    val promptIds =
      readInTransaction {
        entityManager
          .createQuery(
            "select t.promptId from Translation t where t.key.project.id = :p and t.text = 'Hello'",
            java.lang.Long::class.java,
          ).setParameter("p", target.targetProject.id)
          .resultList
      }
    assertThat(promptIds).containsOnlyNulls()
  }

  @Test
  fun `reconciles the default branch and remaps Key and Task branch references`() {
    val branched = ProjectImportBranchedSourceTestData()
    testDataService.saveTestData(branched.root)
    val zip = exportZip(branched.project.id)
    importer.import(ByteArrayInputStream(zip), target.targetProject.id, source.adminUser.id, VERSION)

    val projectId = target.targetProject.id
    readInTransaction {
      val branches =
        entityManager
          .createQuery("select b from Branch b where b.project.id = :p", io.tolgee.model.branching.Branch::class.java)
          .setParameter("p", projectId)
          .resultList
      val defaults = branches.filter { it.isDefault }
      assertThat(defaults).hasSize(1)
      assertThat(branches).allSatisfy { assertThat(it.pending).isFalse() }

      val feature = branches.single { !it.isDefault }
      assertThat(feature.originBranch?.id).isEqualTo(defaults.single().id)

      val task =
        entityManager
          .createQuery("select t from Task t where t.project.id = :p", io.tolgee.model.task.Task::class.java)
          .setParameter("p", projectId)
          .resultList
          .single()
      assertThat(task.branch?.id).isEqualTo(feature.id)
      assertThat(task.originBranchName).isEqualTo("feature")
    }

    assertThat(keyBranchName(projectId, branched.featureKeyName)).isEqualTo("feature")
    assertThat(keyBranchName(projectId, branched.defaultKeyName)).isEqualTo("main")
  }

  private fun importSourceOntoTarget() {
    val zip = exportZip(source.project.id)
    importer.import(ByteArrayInputStream(zip), target.targetProject.id, source.adminUser.id, VERSION)
  }

  // The importer commits in its own transaction and this test class is not transactional, so post-import
  // reads run here in a fresh session to see the committed result (and keep lazy associations initialized).
  private fun <T> readInTransaction(fn: () -> T): T = executeInNewTransaction { fn() }

  private fun singleScreenshot(projectId: Long): Screenshot =
    readInTransaction {
      entityManager
        .createQuery(
          "select distinct r.screenshot from KeyScreenshotReference r where r.key.project.id = :p",
          Screenshot::class.java,
        ).setParameter("p", projectId)
        .resultList
        .single()
    }

  /** Re-zips [zip], dropping every entry whose name matches [drop] — used to simulate a missing blob. */
  private fun rezipWithout(
    zip: ByteArray,
    drop: (String) -> Boolean,
  ): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zos ->
      ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
        generateSequence { zis.nextEntry }.filterNot { it.isDirectory || drop(it.name) }.forEach { entry ->
          val bytes = zis.readAllBytes()
          zos.putNextEntry(ZipEntry(entry.name))
          zos.write(bytes)
          zos.closeEntry()
        }
      }
    }
    return out.toByteArray()
  }

  private fun exportZip(projectId: Long): ByteArray {
    val tempFile = exporter.exportToTempFile(projectId, VERSION).path
    try {
      return Files.readAllBytes(tempFile)
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  /** Attribute maps for one OWNED type, minus the attributes the mirror intentionally does not preserve. */
  private fun comparableAttrs(
    zip: ByteArray,
    type: String,
  ): List<Map<String, Any?>> = entities(zip, type).map { record -> record.attrs.filterKeys { isComparable(type, it) } }

  private fun isComparable(
    type: String,
    attr: String,
  ): Boolean {
    if (attr in EXCLUDED_ATTRS) return false
    // Branch.pending is reset to false on import, so it intentionally won't round-trip.
    if (type == "Branch" && attr == "pending") return false
    return true
  }

  private fun ownedTypeNames(zip: ByteArray): Set<String> =
    readZip(zip)
      .keys
      .filter { it.startsWith(ExportZipLayout.ENTITIES_DIR) }
      .map { it.removePrefix(ExportZipLayout.ENTITIES_DIR).removeSuffix(".json") }
      .toSet()

  private fun entities(
    zip: ByteArray,
    type: String,
  ): List<SerializedEntity> {
    val bytes = readZip(zip)[ExportZipLayout.entityPath(type)] ?: return emptyList()
    return objectMapper.readValue(bytes, object : TypeReference<List<SerializedEntity>>() {})
  }

  private fun readZip(zip: ByteArray): Map<String, ByteArray> =
    ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
      generateSequence { stream.nextEntry }
        .filterNot { it.isDirectory }
        .associate { it.name to stream.readAllBytes() }
    }

  private fun keyNames(projectId: Long): List<String> =
    projectStrings(projectId, "select k.name from Key k where k.project.id = :p")

  private fun labelNames(projectId: Long): List<String> =
    projectStrings(projectId, "select l.name from Label l where l.project.id = :p")

  private fun branchNames(projectId: Long): List<String> =
    projectStrings(projectId, "select b.name from Branch b where b.project.id = :p")

  private fun languageTags(projectId: Long): List<String> =
    projectStrings(projectId, "select l.tag from Language l where l.project.id = :p")

  private fun translationTexts(projectId: Long): List<String?> =
    readInTransaction {
      entityManager
        .createQuery("select t.text from Translation t where t.key.project.id = :p", String::class.java)
        .setParameter("p", projectId)
        .resultList
    }

  private fun labelNamesOnTranslation(
    projectId: Long,
    text: String,
  ): List<String> =
    readInTransaction {
      entityManager
        .createQuery(
          "select l.name from Translation t join t.labels l where t.key.project.id = :p and t.text = :t",
          String::class.java,
        ).setParameter("p", projectId)
        .setParameter("t", text)
        .resultList
    }

  private fun tagNamesOnKey(
    projectId: Long,
    keyName: String,
  ): List<String> =
    readInTransaction {
      entityManager
        .createQuery(
          "select tag.name from Key k join k.keyMeta km join km.tags tag where k.project.id = :p and k.name = :n",
          String::class.java,
        ).setParameter("p", projectId)
        .setParameter("n", keyName)
        .resultList
    }

  private fun taskKeyCount(
    projectId: Long,
    taskName: String,
  ): Long =
    readInTransaction {
      entityManager
        .createQuery(
          "select count(tk) from TaskKey tk where tk.task.project.id = :p and tk.task.name = :n",
          java.lang.Long::class.java,
        ).setParameter("p", projectId)
        .setParameter("n", taskName)
        .singleResult
        .toLong()
    }

  private fun screenshotCount(projectId: Long): Long =
    readInTransaction {
      entityManager
        .createQuery(
          "select count(distinct r.screenshot) from KeyScreenshotReference r where r.key.project.id = :p",
          java.lang.Long::class.java,
        ).setParameter("p", projectId)
        .singleResult
        .toLong()
    }

  private fun keyScreenshotReferenceCount(projectId: Long): Long =
    readInTransaction {
      entityManager
        .createQuery(
          "select count(r) from KeyScreenshotReference r where r.key.project.id = :p",
          java.lang.Long::class.java,
        ).setParameter("p", projectId)
        .singleResult
        .toLong()
    }

  private fun projectStrings(
    projectId: Long,
    jpql: String,
  ): List<String> =
    readInTransaction {
      entityManager
        .createQuery(jpql, String::class.java)
        .setParameter("p", projectId)
        .resultList
    }

  private fun branchCount(projectId: Long): Long =
    readInTransaction {
      entityManager
        .createQuery("select count(b) from Branch b where b.project.id = :p", java.lang.Long::class.java)
        .setParameter("p", projectId)
        .singleResult
        .toLong()
    }

  private fun nativeCount(
    table: String,
    column: String,
    value: Long,
  ): Long =
    readInTransaction {
      (
        entityManager
          .createNativeQuery("SELECT count(*) FROM $table WHERE $column = :v")
          .setParameter("v", value)
          .singleResult as Number
      ).toLong()
    }

  private fun commentAuthorUsername(
    projectId: Long,
    text: String,
  ): String =
    readInTransaction {
      entityManager
        .createQuery(
          "select tc.author.username from TranslationComment tc " +
            "where tc.translation.key.project.id = :p and tc.text = :t",
          String::class.java,
        ).setParameter("p", projectId)
        .setParameter("t", text)
        .singleResult
    }

  private fun singleSuggestion(
    projectId: Long,
    translation: String,
  ): TranslationSuggestion =
    readInTransaction {
      entityManager
        .createQuery(
          "select s from TranslationSuggestion s join fetch s.key join fetch s.language left join fetch s.author " +
            "where s.project.id = :p and s.translation = :t",
          TranslationSuggestion::class.java,
        ).setParameter("p", projectId)
        .setParameter("t", translation)
        .singleResult
    }

  private fun suggestionTexts(projectId: Long): List<String> =
    projectStrings(projectId, "select s.translation from TranslationSuggestion s where s.project.id = :p")

  private fun qaIssues(
    projectId: Long,
    keyName: String,
    languageTag: String,
  ): List<TranslationQaIssue> =
    readInTransaction {
      entityManager
        .createQuery(
          "select q from TranslationQaIssue q where q.translation.key.project.id = :p " +
            "and q.translation.key.name = :k and q.translation.language.tag = :l",
          TranslationQaIssue::class.java,
        ).setParameter("p", projectId)
        .setParameter("k", keyName)
        .setParameter("l", languageTag)
        .resultList
    }

  private fun qaIssueReplacements(projectId: Long): List<String?> =
    readInTransaction {
      entityManager
        .createQuery(
          "select q.replacement from TranslationQaIssue q where q.translation.key.project.id = :p",
          String::class.java,
        ).setParameter("p", projectId)
        .resultList
    }

  private fun qaChecksStale(
    projectId: Long,
    keyName: String,
  ): Boolean =
    readInTransaction {
      entityManager
        .createQuery(
          "select t.qaChecksStale from Translation t " +
            "where t.key.project.id = :p and t.key.name = :k and t.language.tag = 'en'",
          java.lang.Boolean::class.java,
        ).setParameter("p", projectId)
        .setParameter("k", keyName)
        .singleResult
        .booleanValue()
    }

  private fun keyBranchName(
    projectId: Long,
    keyName: String,
  ): String? =
    readInTransaction {
      entityManager
        .createQuery(
          "select k.branch.name from Key k where k.project.id = :p and k.name = :n",
          String::class.java,
        ).setParameter("p", projectId)
        .setParameter("n", keyName)
        .resultList
        .firstOrNull()
    }

  private fun projectAvatarHash(projectId: Long): String? =
    readInTransaction {
      entityManager
        .createQuery("select p.avatarHash from Project p where p.id = :p", String::class.java)
        .setParameter("p", projectId)
        .singleResult
    }

  private fun activityRevisionCount(): Long =
    readInTransaction {
      (
        entityManager
          .createNativeQuery("SELECT count(*) FROM activity_revision")
          .singleResult as Number
      ).toLong()
    }

  private fun screenshotPathFromModel(screenshot: Screenshot): String =
    "${ScreenshotService.SCREENSHOTS_STORAGE_FOLDER_NAME}/${screenshot.filename}"

  companion object {
    private const val VERSION = "9.9.9-import-test"

    // A real 1x1 PNG: the avatar restore re-decodes the bytes (AvatarService/ImageConverter) to compute a
    // thumbnail, so non-image bytes would fail.
    private val AVATAR_BYTES: ByteArray =
      Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
      )
    private val EXCLUDED_ATTRS = setOf("createdAt", "updatedAt")
  }
}
