package io.tolgee.service.key

import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace
import io.tolgee.repository.NamespaceRepository
import io.tolgee.util.tryUntilItDoesntBreakConstraint
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
    val namespaceIds = namespaces.mapNotNull { it?.id }
    val counts = namespaceRepository.getKeysInNamespaceCount(namespaceIds).associate { it[0] to it[1] }
    namespaceIds.forEach {
      if (counts[it] == 0L || counts[it] == null) {
        deleteNamespace(it)
      }
    }
  }

  fun deleteNamespace(namespace: Namespace) {
    namespaceRepository.delete(namespace)
  }

  fun deleteNamespace(namespaceId: Long) {
    namespaceRepository.deleteById(namespaceId)
  }

  fun updateNamespace(key: Key, newNamespace: String?) {
    val oldNamespace = key.namespace
    key.namespace = findOrCreateNamespace(newNamespace, key.project.id)
    if (oldNamespace != null) {
      deleteNamespaceIfUnused(oldNamespace)
    }
  }

  fun deleteNamespaceIfUnused(namespace: Namespace?) {
    namespace ?: return
    val count = getKeysInNamespaceCount(namespace)
    if (count == 0L) {
      deleteNamespace(namespace)
    }
  }

  fun save(namespace: Namespace) {
    namespaceRepository.save(namespace)
  }

  private fun findNamespace(name: String?, projectId: Long): Namespace? {
    name ?: return null
    return namespaceRepository.findByNameAndProjectId(name, projectId)
  }

  fun findOrCreateNamespace(name: String?, projectId: Long): Namespace? {
    return tryUntilItDoesntBreakConstraint {
      findNamespace(name, projectId) ?: createNamespace(name, projectId)
    }
  }

  private fun createNamespace(name: String?, projectId: Long): Namespace? {
    name ?: return null
    return Namespace(
      name = name,
      project = entityManager.getReference(Project::class.java, projectId)
    ).apply {
      namespaceRepository.save(this)
    }
  }
}
