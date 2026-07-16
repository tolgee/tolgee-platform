package io.tolgee.cache

import io.netty.buffer.ByteBufUtil
import io.tolgee.configuration.EnumNameKryo5Codec
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.codec.Kryo5Codec

class EnumNameKryo5CodecTest {
  private val codec = EnumNameKryo5Codec()

  @Test
  fun `writes enum constant name instead of ordinal`() {
    assertThat(codec.encodeToBytes(Scope.ADMIN).stripKryoHighBits()).contains("ADMIN")
    assertThat(Kryo5Codec().encodeToBytes(Scope.ADMIN).stripKryoHighBits()).doesNotContain("ADMIN")
  }

  @Test
  fun `does not read entries written by the ordinal codec`() {
    assertThatThrownBy { codec.decode(Kryo5Codec().encode(Scope.ADMIN)) }.isInstanceOf(Throwable::class.java)
    assertThatThrownBy {
      codec.decode(Kryo5Codec().encode(arrayOf(Scope.ADMIN, Scope.KEYS_EDIT)))
    }.isInstanceOf(Throwable::class.java)
  }

  @Test
  fun `round trips enums standalone and nested in collections`() {
    val scopes = listOf(Scope.ADMIN, Scope.KEYS_EDIT)
    assertThat(codec.roundTrip(Scope.ADMIN)).isEqualTo(Scope.ADMIN)
    assertThat(codec.roundTrip(ArrayList(scopes))).isEqualTo(scopes)
    assertThat(codec.roundTrip(scopes.toTypedArray())).isEqualTo(scopes.toTypedArray())
  }

  @Test
  fun `keeps writing names when Redisson rebinds the codec to a classloader`() {
    val rebound = BaseCodec.copy(javaClass.classLoader, codec)

    assertThat(rebound).isInstanceOf(EnumNameKryo5Codec::class.java)
    assertThat(rebound).isNotSameAs(codec)
    assertThat(rebound.encodeToBytes(Scope.ADMIN).stripKryoHighBits()).contains("ADMIN")
    assertThat(rebound.roundTrip(Scope.ADMIN)).isEqualTo(Scope.ADMIN)
  }

  private fun Codec.encode(value: Any) = valueEncoder.encode(value)

  private fun Codec.decode(buffer: io.netty.buffer.ByteBuf) = valueDecoder.decode(buffer, State())

  private fun Codec.encodeToBytes(value: Any): ByteArray = ByteBufUtil.getBytes(encode(value))

  private fun Codec.roundTrip(value: Any): Any? = decode(encode(value))

  private fun ByteArray.stripKryoHighBits(): String {
    val ascii = map { (it.toInt() and 0x7F).toByte() }.toByteArray()
    return String(ascii, Charsets.US_ASCII)
  }
}
