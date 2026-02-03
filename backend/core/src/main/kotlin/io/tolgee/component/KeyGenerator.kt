package io.tolgee.component

import com.google.common.io.BaseEncoding
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.SecureRandom

@Component
class KeyGenerator(
  private val random: SecureRandom,
) {
  fun generate(bits: Int = 256): String {
    return BigInteger(bits, random).toString(32)
  }

  fun hash(it: String): String {
    val sha = DigestUtils.sha256(it.toByteArray())
    return BaseEncoding.base64().encode(sha)
  }
}
