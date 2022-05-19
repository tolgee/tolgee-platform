package io.tolgee.service

import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.repository.TagRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
  private val tagRepository: TagRepository,
  private val keyMetaService: KeyMetaService
) {
  fun tagKey(key: Key, tagName: String): Tag {
    val keyMeta = keyMetaService.getOrCreateForKey(key)
    val tag = find(key.project!!, tagName)?.let {
      if (!keyMeta.tags.contains(it)) {
        it.keyMetas.add(keyMeta)
        keyMeta.tags.add(it)
      }
      it
    } ?: let {
      Tag().apply {
        project = key.project!!
        keyMetas.add(keyMeta)
        name = tagName
        keyMeta.tags.add(this)
      }
    }

    if (tag.name.length > 100) {
      throw BadRequestException(io.tolgee.constants.Message.TAG_TOO_LOG)
    }

    tagRepository.save(tag)
    keyMetaService.save(keyMeta)
    return tag
  }

  fun remove(key: Key, tag: Tag) {
    key.keyMeta?.let { keyMeta ->
      tag.keyMetas.remove(keyMeta)
      keyMeta.tags.remove(tag)
      tagRepository.save(tag)
      keyMetaService.save(keyMeta)
      if (tag.keyMetas.size < 1) {
        tagRepository.delete(tag)
      }
    }
  }

  @Transactional
  fun updateTags(key: Key, newTags: List<String>) {
    key.keyMeta?.tags?.forEach { oldTag ->
      if (newTags.find { oldTag.name == it } == null) {
        this.remove(key, oldTag)
      }
    }
    newTags.forEach { tagName ->
      this.tagKey(key, tagName)
    }
  }

  fun getProjectTags(projectId: Long, search: String? = null, pageable: Pageable): Page<Tag> {
    return tagRepository.findAllByProject(projectId, search, pageable)
  }

  fun getTagsForKeyIds(keyIds: Iterable<Long>): Map<Long, List<Tag>> {
    val keys = tagRepository.getKeysWithTags(keyIds)
    return keys.associate { key -> key.id to (key.keyMeta?.tags?.toList() ?: listOf()) }
  }

  fun saveAll(entities: Iterable<Tag>) {
    tagRepository.saveAll(entities)
  }

  fun find(id: Long): Tag? {
    return tagRepository.findById(id).orElse(null)
  }

  fun get(id: Long): Tag {
    return find(id) ?: throw NotFoundException()
  }

  fun find(project: Project, tagName: String): Tag? {
    return tagRepository.findByProjectAndName(project, tagName)
  }

  fun deleteAllByImportKeyIdIn(importKeyIds: List<Long>) {
    val importKeys = tagRepository.getImportKeysWithTags(importKeyIds)
    deleteAllTagsForKeys(importKeys)
  }

  fun deleteAllByKeyIdIn(keyIds: Collection<Long>) {
    val keys = tagRepository.getKeysWithTags(keyIds)
    deleteAllTagsForKeys(keys)
  }

  private fun deleteAllTagsForKeys(keys: Iterable<WithKeyMeta>) {
    val tagIds = keys.flatMap { it.keyMeta?.tags?.map { it.id } ?: listOf() }.toSet()
    // get tags with fetched keyMetas
    val tagKeyMetasMap = tagRepository.getTagsWithKeyMetas(tagIds).map { it.id to it.keyMetas }.toMap()
    keys.forEach { key ->
      key.keyMeta?.let { keyMeta ->
        keyMeta.tags.forEach { tag ->
          // remove from tagsKeyMetas to find out whether to delete the tag
          val tagKeyMetas = tagKeyMetasMap[tag.id]
          tagKeyMetas?.removeIf { it.id == keyMeta.id }
          if (tagKeyMetas?.isEmpty() != false) {
            tagRepository.delete(tag)
          }
        }
        keyMeta.tags.clear()
        keyMetaService.save(keyMeta)
      }
    }
  }
}
