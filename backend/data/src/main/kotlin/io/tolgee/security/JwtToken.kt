package io.tolgee.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import java.security.Key
import java.util.*

class JwtToken(private val value: String, private val key: Key?) {
  private lateinit var _parsed: Jws<Claims>
  private val parsed: Jws<Claims>
    get() {
      if (!this::_parsed.isInitialized)
        _parsed = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(value)
      return _parsed
    }

  val content: String
    get() = parsed.body.subject

  val id: Long
    get() = content.toLong()

  val issuedAt: Date
    get() = parsed.body.issuedAt

  override fun toString(): String {
    return value
  }
}
