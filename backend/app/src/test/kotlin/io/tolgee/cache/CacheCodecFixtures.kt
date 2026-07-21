package io.tolgee.cache

import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.model.enums.Scope

internal val permissionDtoFixture =
  PermissionDto(
    id = 1,
    userId = 2,
    invitationId = null,
    scopes = arrayOf(Scope.ADMIN, Scope.KEYS_EDIT),
    projectId = 3,
    organizationId = null,
    type = null,
    granular = true,
    viewLanguageIds = null,
    stateChangeLanguageIds = null,
    suggestLanguageIds = null,
    suggestManageLanguageIds = null,
  )

internal val apiKeyDtoWithoutExpiryFixture =
  ApiKeyDto(
    id = 1,
    hash = "hash",
    expiresAt = null,
    projectId = 2,
    userAccountId = 3,
    scopes = setOf(Scope.ADMIN),
  )

/** Kryo terminates an ASCII string by setting the high bit on its last byte, so the name never matches verbatim. */
internal fun ByteArray.stripKryoHighBits(): String =
  String(map { (it.toInt() and 0x7F).toByte() }.toByteArray(), Charsets.US_ASCII)
