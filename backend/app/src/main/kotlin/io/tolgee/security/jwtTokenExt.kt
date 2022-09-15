package io.tolgee.security

import io.jsonwebtoken.Claims

const val JWT_TOKEN_SUPER_EXPIRATION_CLAIM = "superExpiration"

var Claims.superExpiration: Long?
  set(value) = this.set(JWT_TOKEN_SUPER_EXPIRATION_CLAIM, value)
  get() = this[JWT_TOKEN_SUPER_EXPIRATION_CLAIM] as? Long
