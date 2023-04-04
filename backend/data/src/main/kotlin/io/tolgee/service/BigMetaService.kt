package io.tolgee.service

import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.BigMetaItemDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.BigMeta
import io.tolgee.model.views.BigMetaView
import io.tolgee.repository.BigMetaRepository
import io.tolgee.service.key.KeyService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

@Service
class BigMetaService(
  private val bigMetaRepository: BigMetaRepository,
  private val keyService: KeyService,
  private val transactionManager: PlatformTransactionManager
) {
  fun save(bigMeta: BigMeta): BigMeta {
    return bigMetaRepository.save(bigMeta)
  }

  fun store(data: BigMetaDto, project: Project): List<BigMetaView> {
    return tryUntilItDoesntBreakConstraint {
      executeInNewTransaction(transactionManager) {
        data.items.map { item ->
          val bigMeta = findOrCreate(item, project).apply {
            contextData = item.contextData
          }
          getView(save(bigMeta))
        }
      }
    }
  }

  fun findOrCreate(data: BigMetaItemDto, project: Project): BigMeta {
    return this.find(
      keyName = data.keyName,
      namespace = data.namespace,
      location = data.location,
      project = project
    ) ?: BigMeta().apply {
      keyName = data.keyName
      namespace = data.namespace
      location = data.location
      this.project = project
    }
  }

  fun getView(bigMeta: BigMeta): BigMetaView {
    return object : BigMetaView {
      override val bigMeta: BigMeta
        get() = bigMeta
      override val key: Key?
        get() = keyService.find(bigMeta.project.id, bigMeta.keyName, bigMeta.namespace)
    }
  }

  fun find(keyName: String, namespace: String?, location: String, project: Project): BigMeta? {
    return bigMetaRepository
      .findOneByKeyNameAndNamespaceAndLocationAndProject(
        keyName = keyName,
        namespace = namespace,
        location = location,
        project = project
      )
  }

  fun get(id: Long): BigMeta {
    return find(id) ?: throw NotFoundException()
  }

  fun find(id: Long): BigMeta? {
    return this.bigMetaRepository.findById(id).orElse(null)
  }

  fun delete(bigMeta: BigMeta) {
    bigMetaRepository.delete(bigMeta)
  }

  fun getAll(projectId: Long, pageable: Pageable): Page<BigMetaView> {
    return this.bigMetaRepository.findAllByProjectId(projectId, pageable)
  }

  fun getAllForKeyPaged(projectId: Long, keyId: Long, pageable: Pageable): Page<BigMetaView> {
    return this.bigMetaRepository.getMetas(projectId, keyId, pageable)
  }

  fun getAllForKey(keyId: Long): List<BigMeta> {
    return this.bigMetaRepository.getMetas(keyId)
  }
}
