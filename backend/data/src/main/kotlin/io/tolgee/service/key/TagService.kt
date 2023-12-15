package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.repository.TagRepository
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
  private val tagRepository: TagRepository,
  private val keyMetaService: KeyMetaService,
  @Lazy
  private val keyService: KeyService,
  private val entityManager: EntityManager
) : Logging {
  fun tagKey(key: Key, tagName: String): Tag {
    val keyMeta = keyMetaService.getOrCreateForKey(key)
    val tag = find(key.project, tagName)?.let {
      if (!keyMeta.tags.contains(it)) {
        it.keyMetas.add(keyMeta)
        keyMeta.tags.add(it)
      }
      it
    } ?: let {
      Tag().apply {
        project = key.project
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

  /**
   * @param map key entity to list of tags
   */
  fun tagKeys(map: Map<Key, List<String>>) {
    if (map.isEmpty()) {
      return
    }

    val keysWithTags = keyService.getKeysWithTags(map.keys)
    tagKeys(keysWithTags, map.mapKeys { it.key.id })
  }

  /**
   * @param map keyId entity to list of tags
   */
  fun tagKeysById(map: Map<Long, List<String>>) {
    if (map.isEmpty()) {
      return
    }

    val keysWithTags = keyService.getKeysWithTagsById(map.keys)
    tagKeys(keysWithTags, map)
  }

  private fun tagKeys(keysWithFetchedTags: Iterable<Key>, map: Map<Long, List<String>>) {
    val keysByIdMap = keysWithFetchedTags.associateBy { it.id }
    val projectId = getSingleProjectId(keysByIdMap)

    val existingTags =
      this.getFromProject(projectId, map.values.flatten().toSet()).associateBy { it.name }.toMutableMap()

    map.forEach { (keyId, tagsToAdd) ->
      tagsToAdd.forEach { tagToAdd ->
        val keyWithData = keysByIdMap[keyId] ?: throw NotFoundException(Message.KEY_NOT_FOUND)
        val keyMeta = keyMetaService.getOrCreateForKey(keyWithData)
        val tag = existingTags[tagToAdd]?.let {
          if (!keyMeta.tags.contains(it)) {
            it.keyMetas.add(keyMeta)
            keyMeta.tags.add(it)
          }
          it
        } ?: let {
          Tag().apply {
            project = keysByIdMap[keyId]?.project ?: throw NotFoundException(Message.KEY_NOT_FOUND)
            keyMetas.add(keyMeta)
            name = tagToAdd
            keyMeta.tags.add(this)
            existingTags[tagToAdd] = this
          }
        }
        tagRepository.save(tag)
        keyMetaService.save(keyMeta)
      }
    }
  }

  fun untagKeys(map: Map<Long, List<String>>) {
    if (map.isEmpty()) {
      return
    }

    val keysWithTags = keyService.getKeysWithTagsById(map.keys)
    untagKeys(keysWithTags, map)
  }

  private fun untagKeys(keysWithFetchedTags: Iterable<Key>, map: Map<Long, List<String>>) {
    val keysByIdMap = keysWithFetchedTags.associateBy { it.id }

    map.forEach { (keyId, tagsToRemove) ->
      keysByIdMap[keyId]?.keyMeta?.let { keyMeta ->
        keyMeta.tags.removeIf { tagsToRemove.contains(it.name) }
        keyMetaService.save(keyMeta)
      }
    }

    keysWithFetchedTags.map { it.project.id }.toSet().forEach {
      this.removeUnusedTags(it)
    }
  }

  private fun removeUnusedTags(projectId: Long) {
    tagRepository.deleteAllUnused(projectId)
  }

  private fun getSingleProjectId(keysWithTags: Map<Long, Key>): Long {
    val projectIds = keysWithTags.map { it.value.project.id }.toSet()

    if (projectIds.size > 1) {
      throw BadRequestException(Message.MULTIPLE_PROJECTS_NOT_SUPPORTED)
    }

    if (projectIds.isEmpty()) {
      throw IllegalStateException("No project found? This should not happen.")
    }

    return projectIds.single()
  }

  private fun getFromProject(projectId: Long, tags: Collection<String>): List<Tag> {
    return tagRepository.findAllByProject(projectId, tags)
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

  fun getAllFromProject(projectId: Long): List<Tag> {
    return tagRepository.findAllByProjectId(projectId)
  }

  fun find(project: Project, tagName: String): Tag? {
    return tagRepository.findByProjectAndName(project, tagName)
  }

  fun deleteAllByImportKeyIdIn(importKeyIds: List<Long>) {
    val importKeys = tagRepository.getImportKeysWithTags(importKeyIds)
    deleteAllTagsForKeys(importKeys)
  }

  fun deleteAllByKeyIdIn(keyIds: Collection<Long>) {
    val keys = traceLogMeasureTime("tagService: deleteAllByKeyIdIn: getKeysWithTags") {
      tagRepository.getKeysWithTags(keyIds)
    }
    deleteAllTagsForKeys(keys)
  }

  private fun deleteAllTagsForKeys(keys: Iterable<WithKeyMeta>) {
    val tagIds = keys.flatMap { it.keyMeta?.tags?.map { it.id } ?: listOf() }.toSet()
    // get tags with fetched keyMetas
    val tagKeyMetasMap =
      traceLogMeasureTime("tagService: deleteAllTagsForKeys: getTagsWithKeyMetas") {
        tagRepository.getTagsWithKeyMetas(tagIds).associate {
          it.id to it.keyMetas
        }
      }
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

  /**
   * We don't need to store history or handle events when deleting whole project.
   * So we can go for native query.
   */
  fun deleteAllByProject(projectId: Long) {
    entityManager.createNativeQuery(
      """
      delete from key_meta_tags kmt 
        where kmt.key_metas_id in 
        (select km.id from key_meta km join key k on km.key_id = k.id where k.project_id = :projectId)"""
    ).setParameter("projectId", projectId).executeUpdate()
    entityManager.createNativeQuery(
      """
      delete from tag where project_id = :projectId
      """
    ).setParameter("projectId", projectId).executeUpdate()
  }
}
