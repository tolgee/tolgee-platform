package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Tag
import io.tolgee.model.task.Task
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Date

/**
 * A project graph covering the export serializer's load-bearing cases: types unreachable by a graph
 * walk (ProjectQaConfig, LanguageQaConfig, an unassigned Label), one Screenshot shared by two keys
 * with real image bytes, a project avatar, a user-authored comment, a `@DoNotExport` promptId, and
 * both a soft-deleted key and a live key pointing at a soft-deleted branch.
 */
class ProjectExportImportTestData(
  projectName: String = "export-test-project",
) : BaseTestData(projectName = projectName) {
  lateinit var adminUser: UserAccount
  lateinit var sharedScreenshot: Screenshot
  val distinctivePromptId = 868686868686L
  val softDeletedKeyName = "trashed-key"
  val softDeletedTranslationText = "should-not-be-exported"
  val assignedLabelName = "assigned-label"
  val unassignedLabelName = "unassigned-label"
  val avatarHash = "export-test-avatar-hash"

  // A real 1x1 PNG: the export stores the bytes as-is, but the import decodes them to regenerate
  // thumbnails (ImageConverter/ImageIO), so non-image bytes would fail the round-trip.
  val screenshotImageBytes: ByteArray =
    Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
    )
  val commentText = "Please review"
  val keyOnDeletedBranchName = "key-on-deleted-branch"
  val deletedBranchName = "deleted-branch"
  val taskOnDeletedLanguageName = "task-on-deleted-language"
  val keyForExcludedTaskName = "live-key-for-excluded-task"
  val liveTaskName = "live-task"
  val keyForLiveTaskName = "live-key-for-live-task"
  val liveNamespaceName = "main-ns"
  val liveTagName = "live-tag"
  val liveKeyMetaDescription = "live-key-meta"
  val liveCodeReferencePath = "src/Live.kt"
  val trashedKeyMetaDescription = "trashed-key-meta"
  val trashedCodeReferencePath = "src/Trashed.kt"
  val trashedTranslationCommentText = "trashed-translation-comment"
  lateinit var assignedLabel: Label
  lateinit var taskOnDeletedLanguage: Task
  lateinit var liveTask: Task

  init {
    projectBuilder.self.avatarHash = avatarHash
    root.addUserAccount {
      username = "export-admin@admin.com"
      name = "Export Admin"
      role = UserAccount.Role.ADMIN
      adminUser = this
    }

    projectBuilder.apply {
      setQaConfig { }
      englishLanguageBuilder.setQaConfig { }

      addKey(keyName = "greeting").build {
        addTranslation("en", "Hello").self.promptId = distinctivePromptId
      }

      lateinit var labeledTranslation: Translation
      addKey(keyName = "labeled").build {
        addTranslation("en", "Labeled").build {
          labeledTranslation = self
          addComment {
            text = commentText
            author = this@ProjectExportImportTestData.user
          }
        }
      }
      assignedLabel =
        addLabel {
          name = assignedLabelName
          color = "#FF0000"
          project = projectBuilder.self
        }.self
      labeledTranslation.addLabel(assignedLabel)
      addLabel {
        name = unassignedLabelName
        color = "#00FF00"
        project = projectBuilder.self
      }

      addKey(namespace = liveNamespaceName, keyName = "rich-key") {
        addTranslation("en", "Rich")
        addMeta {
          description = liveKeyMetaDescription
          tags.add(
            Tag().apply {
              project = projectBuilder.self
              name = liveTagName
            },
          )
          addCodeReference(this@ProjectExportImportTestData.user) {
            path = liveCodeReferencePath
            line = 10L
          }
        }
      }

      addKey(keyName = "screenshot-key-1").build {
        val screenshotBuilder = addScreenshot { }
        screenshotBuilder.image = ByteArrayOutputStream().apply { write(screenshotImageBytes) }
        sharedScreenshot = screenshotBuilder.self
      }
      addKey(keyName = "screenshot-key-2").build {
        val secondKey = self
        projectBuilder.addScreenshotReference {
          screenshot = sharedScreenshot
          key = secondKey
        }
      }

      val deletedBranch =
        addBranch {
          name = deletedBranchName
          isDefault = false
          deletedAt = Date()
          project = projectBuilder.self
        }.self
      addKey(keyName = keyOnDeletedBranchName).build {
        self.branch = deletedBranch
      }

      addKey {
        name = softDeletedKeyName
        deletedAt = Date()
      }.build {
        addTranslation("en", softDeletedTranslationText).build {
          addComment {
            text = trashedTranslationCommentText
            author = this@ProjectExportImportTestData.user
          }
        }
        addMeta {
          description = trashedKeyMetaDescription
          tags.add(
            Tag().apply {
              project = projectBuilder.self
              name = "trashed-tag"
            },
          )
          addCodeReference(this@ProjectExportImportTestData.user) { path = trashedCodeReferencePath }
        }
      }

      val deletedLanguage =
        addLanguage {
          tag = "zz-deleted"
          name = "Deleted language"
          originalName = "Deleted language"
          deletedAt = Date()
        }.self
      taskOnDeletedLanguage =
        addTask {
          name = taskOnDeletedLanguageName
          number = 1
          language = deletedLanguage
          project = projectBuilder.self
        }.self
      val liveKeyForExcludedTask = addKey(keyName = keyForExcludedTaskName).self
      addTaskKey {
        task = taskOnDeletedLanguage
        key = liveKeyForExcludedTask
      }

      liveTask =
        addTask {
          name = liveTaskName
          number = 2
          language = englishLanguage
          project = projectBuilder.self
        }.self
      val liveKeyForLiveTask = addKey(keyName = keyForLiveTaskName).self
      addTaskKey {
        task = liveTask
        key = liveKeyForLiveTask
      }
    }
  }
}
