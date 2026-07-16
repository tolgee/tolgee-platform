package io.tolgee.cache

import io.netty.buffer.ByteBuf
import io.tolgee.configuration.EnumNameKryo5Codec
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.codec.Kryo5Codec

class EnumNameKryo5CodecTest {
  private val codec = EnumNameKryo5Codec()

  private fun Codec.encodeToBytes(value: Any): ByteArray {
    val buffer = valueEncoder.encode(value)
    val bytes = ByteArray(buffer.readableBytes())
    buffer.getBytes(buffer.readerIndex(), bytes)
    return bytes
  }

  private fun Codec.roundTrip(value: Any): Any? {
    val buffer: ByteBuf = valueEncoder.encode(value)
    return valueDecoder.decode(buffer, State())
  }

  /** Kryo terminates an ASCII string by setting the high bit of its last byte, so clear it before matching. */
  private fun ByteArray.asKryoText(): String {
    val ascii = map { (it.toInt() and 0x7F).toByte() }.toByteArray()
    return String(ascii, Charsets.US_ASCII)
  }

  @Test
  fun `writes enum constant name instead of ordinal`() {
    assertThat(codec.encodeToBytes(Scope.ADMIN).asKryoText()).contains("ADMIN")
    assertThat(Kryo5Codec().encodeToBytes(Scope.ADMIN).asKryoText()).doesNotContain("ADMIN")
  }

  @Test
  fun `round trips enum value`() {
    assertThat(codec.roundTrip(Scope.ADMIN)).isEqualTo(Scope.ADMIN)
  }

  @Test
  fun `round trips enums nested in collections`() {
    val scopes = listOf(Scope.ADMIN, Scope.KEYS_EDIT)
    assertThat(codec.roundTrip(ArrayList(scopes))).isEqualTo(scopes)
    assertThat(codec.roundTrip(scopes.toTypedArray())).isEqualTo(scopes.toTypedArray())
  }
}
