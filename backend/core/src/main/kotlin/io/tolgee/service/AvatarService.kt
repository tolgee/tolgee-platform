package io.tolgee.service

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.dtos.Avatar
import io.tolgee.model.ModelWithAvatar
import io.tolgee.util.ImageConverter
import jakarta.xml.bind.DatatypeConverter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import java.io.InputStream
import java.security.MessageDigest

@Service
class AvatarService(
  private val fileStorage: FileStorage,
  private val tolgeeProperties: TolgeeProperties,
) {
  companion object {
    fun getAvatarPaths(hash: String) =
      Avatar(
        large = "${FileStoragePath.AVATARS}/$hash.png",
        thumbnail = "${FileStoragePath.AVATARS}/$hash-thumb.png",
      )
  }

  fun storeAvatarFiles(
    avatar: InputStream,
    entity: ModelWithAvatar,
    dimension: Dimension? = null,
    thumbnailSize: Int? = null,
  ): String {
    val dim = dimension ?: Dimension(200, 200)
    val thumbSize = thumbnailSize ?: 50
    val avatarBytes = avatar.readAllBytes()
    val converter = ImageConverter(avatarBytes.inputStream())
    val large = converter.getImage(-1f, dim).toByteArray()
    val thumb = converter.getThumbnail(thumbSize).toByteArray()
    val idByteArray = "${entity::class.simpleName}-${entity.id}---".toByteArray()
    val bytesToHash = idByteArray + large
    val hashBinary = MessageDigest.getInstance("SHA-256").digest(bytesToHash)
    val hash = DatatypeConverter.printHexBinary(hashBinary)
    val (largePath, thumbnailPath) = getAvatarPaths(hash)
    fileStorage.storeFile(largePath, large)
    fileStorage.storeFile(thumbnailPath, thumb)
    return hash
  }

  @Transactional
  fun setAvatar(
    entity: ModelWithAvatar,
    avatar: InputStream,
    dimension: Dimension? = null,
    thumbnailSize: Int? = null,
  ) {
    val hash = storeAvatarFiles(avatar, entity, dimension, thumbnailSize)
    removeAvatar(entity)
    entity.avatarHash = hash
  }

  @Transactional
  fun removeAvatar(entity: ModelWithAvatar) {
    unlinkAvatarFiles(entity)
    entity.avatarHash = null
  }

  fun unlinkAvatarFiles(entity: ModelWithAvatar) {
    entity.avatarHash?.let { hash ->
      val (largePath, thumbnailPath) = getAvatarPaths(hash)
      fileStorage.deleteFile(largePath)
      fileStorage.deleteFile(thumbnailPath)
    }
  }

  fun getAvatarLinks(hash: String?): Avatar? {
    return hash?.let {
      val paths = getAvatarPaths(it)
      getAvatarLinks(paths)
    }
  }

  fun getAvatarLinks(paths: Avatar) =
    Avatar(
      tolgeeProperties.fileStorageUrl + "/" + paths.large,
      tolgeeProperties.fileStorageUrl + "/" + paths.thumbnail,
    )
}
