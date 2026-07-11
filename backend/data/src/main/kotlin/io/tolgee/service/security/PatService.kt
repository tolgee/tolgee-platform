package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.PatDto
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.dtos.request.pat.UpdatePatDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.repository.PatRepository
import io.tolgee.util.runSentryCatching
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Service
class PatService(
  private val patRepository: PatRepository,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
  private val cacheManager: CacheManager,
) {
  private val cache: Cache? by lazy { cacheManager.getCache(Caches.PERSONAL_ACCESS_TOKENS) }

  fun find(hash: String): Pat? {
    return patRepository.findByTokenHash(hash)
  }

  fun get(hash: String): Pat {
    return find(hash) ?: throw NotFoundException(Message.PAT_NOT_FOUND)
  }

  fun find(id: Long): Pat? {
    return patRepository.findById(id).orElse(null)
  }

  fun get(id: Long): Pat {
    return find(id) ?: throw NotFoundException(Message.PAT_NOT_FOUND)
  }

  @Cacheable(cacheNames = [Caches.PERSONAL_ACCESS_TOKENS], key = "#hash")
  fun findDto(hash: String): PatDto? {
    return find(hash)?.let { PatDto.fromEntity(it) }
  }

  @CacheEvict(cacheNames = [Caches.PERSONAL_ACCESS_TOKENS], key = "#pat.tokenHash")
  fun save(pat: Pat): Pat {
    if (pat.tokenHash.isBlank()) {
      pat.regenerateToken()
    }
    return this.patRepository.save(pat)
  }

  private fun generateToken(): String {
    return keyGenerator.generate(256)
  }

  fun create(
    dto: CreatePatDto,
    userAccount: UserAccount,
  ): Pat {
    val pat =
      Pat().apply {
        expiresAt = dto.expiresAt.epochToDate()
        description = dto.description
        this.userAccount = userAccount
      }
    return save(pat)
  }

  fun regenerate(
    id: Long,
    expiresAt: Long?,
  ): Pat {
    val pat =
      get(id).apply {
        // Manual cache eviction
        cache?.evict(this.tokenHash)

        this.expiresAt = expiresAt.epochToDate()
        this.regenerateToken()
        save(this)
      }
    return pat
  }

  fun update(
    id: Long,
    updatePatDto: UpdatePatDto,
  ): Pat {
    // Cache eviction: Not necessary, description is not cached
    val pat =
      get(id).apply {
        this.description = updatePatDto.description
      }
    return save(pat)
  }

  fun hashToken(token: String): String {
    return keyGenerator.hash(token)
  }

  @CacheEvict(cacheNames = [Caches.PERSONAL_ACCESS_TOKENS], key = "#pat.tokenHash")
  fun delete(pat: Pat) {
    return patRepository.deleteById(pat.id)
  }

  fun findAll(
    userId: Long,
    pageable: Pageable,
  ): Page<Pat> {
    return patRepository.findAllByUserAccountId(userId, pageable)
  }

  private fun Pat.regenerateToken() {
    val token = generateToken()
    this.token = token
    this.tokenHash = hashToken(token)
  }

  private fun Long?.epochToDate(): Date? {
    return this?.let { Date(it) }
  }

  @Async
  @Transactional
  fun updateLastUsedAsync(patId: Long) {
    // Cache eviction: Not necessary, last usage date is not cached
    runSentryCatching {
      updateLastUsed(patId)
    }
  }

  fun updateLastUsed(patId: Long) {
    // Cache eviction: Not necessary, last usage date is not cached
    patRepository.updateLastUsedById(patId, currentDateProvider.date)
  }
}
