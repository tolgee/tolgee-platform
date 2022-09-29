package io.tolgee.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import java.security.Key
import java.util.*

class JwtToken(private val value: String, private val key: Key?) {

  private val parsed: Jws<Claims> by lazy {
    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(value)
  }

  val content: String
    get() = parsed.body.subject

  val claims: Claims
    get() = parsed.body

  val id: Long
    get() = content.toLong()

  val issuedAt: Date
    get() = parsed.body.issuedAt

  override fun toString(): String {
    return value
  }

  val superExpiration: Long?
    get() = this.claims[JWT_TOKEN_SUPER_EXPIRATION_CLAIM] as? Long

  companion object {
    const val JWT_TOKEN_SUPER_EXPIRATION_CLAIM = "ste"
  }
}
