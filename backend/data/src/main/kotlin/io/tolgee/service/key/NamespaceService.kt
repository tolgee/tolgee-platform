package io.tolgee.service.key

import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace
import io.tolgee.repository.NamespaceRepository
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager

@Service
class NamespaceService(
  private val entityManager: EntityManager,
  private val namespaceRepository: NamespaceRepository
) {
  private fun getKeysInNamespaceCount(namespace: Namespace?): Long? {
    namespace ?: return null
    return namespaceRepository.getKeysInNamespaceCount(listOf(namespace.id)).firstOrNull()?.get(1)
  }

  fun deleteUnusedNamespaces(namespaces: List<Namespace?>) {
    val namespaceIds = namespaces.mapNotNull { it?.id }.toSet()

    val counts = namespaceRepository
      .getKeysInNamespaceCount(namespaceIds)
      .associate { it[0] to it[1] }

    namespaceIds.forEach {
      if (counts[it] == 0L || counts[it] == null) {
        delete(it)
      }
    }
  }

  fun delete(namespace: Namespace) {
    namespaceRepository.delete(namespace)
  }

  fun delete(namespaceId: Long) {
    namespaceRepository.deleteById(namespaceId)
  }

  fun update(key: Key, newNamespace: String?) {
    val oldNamespace = key.namespace
    key.namespace = findOrCreate(newNamespace, key.project.id)
    if (oldNamespace != null) {
      deleteIfUnused(oldNamespace)
    }
  }

  fun deleteIfUnused(namespace: Namespace?) {
    namespace ?: return
    val count = getKeysInNamespaceCount(namespace)
    if (count == 0L) {
      delete(namespace)
    }
  }

  fun save(namespace: Namespace) {
    namespaceRepository.save(namespace)
  }

  fun find(name: String?, projectId: Long): Namespace? {
    name ?: return null
    return namespaceRepository.findByNameAndProjectId(name, projectId)
  }

  fun findOrCreate(name: String?, projectId: Long): Namespace? {
    return tryUntilItDoesntBreakConstraint {
      find(getSafeName(name), projectId) ?: create(name, projectId)
    }
  }

  private fun getSafeName(name: String?) = if (name.isNullOrBlank()) null else name

  private fun create(name: String?, projectId: Long): Namespace? {
    if (name.isNullOrBlank()) {
      return null
    }
    return Namespace(
      name = name,
      project = entityManager.getReference(Project::class.java, projectId)
    ).apply {
      namespaceRepository.save(this)
    }
  }

  fun getAllInProject(projectId: Long) = namespaceRepository.getAllByProjectId(projectId)
  fun getAllInProject(
    projectId: Long,
    pageable: Pageable
  ) = namespaceRepository.getAllByProjectId(projectId, pageable)

  fun saveAll(entities: Collection<Namespace>) {
    namespaceRepository.saveAll(entities)
  }
}
