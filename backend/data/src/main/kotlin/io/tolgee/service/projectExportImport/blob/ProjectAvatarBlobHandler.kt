package io.tolgee.service.projectExportImport.blob

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.model.Project
import io.tolgee.service.AvatarService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/** Exports the project's full-size avatar (no thumbnail), named by project id. */
@Component
class ProjectAvatarBlobHandler(
  private val fileStorage: FileStorage,
) : BlobHandler,
  Logging {
  override val entityClass: KClass<*> = Project::class

  override fun export(entity: Any): List<BlobEntry> {
    val project = entity as Project
    val hash = project.avatarHash ?: return emptyList()
    val sourcePath = AvatarService.getAvatarPaths(hash).large
    if (!fileStorage.fileExists(sourcePath)) {
      logger.warn("Project ${project.id} avatar missing at $sourcePath; exporting without it")
      return emptyList()
    }
    return listOf(BlobEntry(blobName(project.id), fileStorage.readFile(sourcePath)))
  }

  companion object {
    const val AVATAR_BLOB_DIR = "avatar/"

    fun blobName(projectId: Long): String = "$AVATAR_BLOB_DIR$projectId.png"
  }
}
