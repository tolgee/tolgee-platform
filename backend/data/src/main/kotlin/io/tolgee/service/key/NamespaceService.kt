package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.dtos.request.key.UpdateNamespaceDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Namespace
import io.tolgee.repository.NamespaceRepository
import io.tolgee.util.getSafeNamespace
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NamespaceService(
  private val entityManager: EntityManager,
  private val namespaceRepository: NamespaceRepository,
) {
  private fun getKeysInNamespaceCount(namespace: Namespace?): Long? {
    namespace ?: return null
    return getKeysInNamespaceCount(listOf(namespace.id)).values.firstOrNull()
  }

  private fun getKeysInNamespaceCount(namespaceIds: List<Long>): Map<Long, Long> {
    return namespaceRepository.getKeysInNamespaceCount(namespaceIds).associate { it[0] to it[1] }
  }

  fun deleteUnusedNamespaces(namespaces: List<Namespace?>) {
    val namespaceIds = namespaces.mapNotNull { it?.id }.toSet()

    val counts =
      namespaceRepository
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

  fun deleteIfUnused(namespace: Namespace?) {
    namespace ?: return
    deleteIfUnused(listOf(namespace))
  }

  fun deleteIfUnused(namespaces: List<Namespace>) {
    val counts = getKeysInNamespaceCount(namespaces.map { it.id })
    namespaces.forEach {
      if (counts[it.id] == 0L || counts[it.id] == null) {
        delete(it)
      }
    }
  }

  fun save(namespace: Namespace) {
    namespaceRepository.save(namespace)
  }

  fun find(
    name: String?,
    projectId: Long,
  ): Namespace? {
    name ?: return null
    return namespaceRepository.findByNameAndProjectId(name, projectId)
  }

  fun findOrCreate(
    name: String?,
    projectId: Long,
  ): Namespace? {
    return tryUntilItDoesntBreakConstraint {
      find(getSafeNamespace(name), projectId) ?: create(name, projectId)
    }
  }

  fun create(
    name: String?,
    projectId: Long,
  ): Namespace? {
    if (name.isNullOrBlank()) {
      return null
    }
    return Namespace(
      name = name,
      project = entityManager.getReference(Project::class.java, projectId),
    ).apply {
      namespaceRepository.save(this)
    }
  }

  fun getAllInProject(projectId: Long) = namespaceRepository.getAllByProjectId(projectId)

  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ) = namespaceRepository.getByProjectAndSearch(projectId, search, pageable)

  fun saveAll(entities: Collection<Namespace>) {
    namespaceRepository.saveAll(entities)
  }

  fun isDefaultUsed(projectId: Long): Boolean {
    return namespaceRepository.isDefaultUsed(projectId)
  }

  fun get(
    projectId: Long,
    namespaceId: Long,
  ): Namespace {
    return this.find(projectId, namespaceId) ?: throw NotFoundException(Message.NAMESPACE_NOT_FOUND)
  }

  fun get(
    projectId: Long,
    name: String,
  ): Namespace {
    return this.find(projectId, name) ?: throw NotFoundException(Message.NAMESPACE_NOT_FOUND)
  }

  fun find(
    projectId: Long,
    name: String,
  ): Namespace? {
    return namespaceRepository.findOneByProjectIdAndName(projectId, name)
  }

  fun find(
    projectId: Long,
    namespaceId: Long,
  ): Namespace? {
    return namespaceRepository.findOneByProjectIdAndId(projectId, namespaceId)
  }

  fun update(
    namespace: Namespace,
    dto: UpdateNamespaceDto,
  ) {
    this.find(dto.name, namespace.project.id)?.let {
      throw BadRequestException(Message.NAMESPACE_EXISTS)
    }
    namespace.name = dto.name!!
    return save(namespace)
  }

  fun deleteAllByProject(projectId: Long) {
    namespaceRepository.deleteAllByProjectId(projectId)
  }
}
