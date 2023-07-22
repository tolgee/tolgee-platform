package io.tolgee.component.cacheWithExpiration

data class CachedWithExpiration(
  val expiresAt: Long,
  val data: Any?
)
