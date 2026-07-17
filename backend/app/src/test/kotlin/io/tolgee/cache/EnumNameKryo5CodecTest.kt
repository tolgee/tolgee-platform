package io.tolgee.cache

import com.esotericsoftware.kryo.KryoException
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.tolgee.component.EnumNameKryo5Codec
import io.tolgee.model.enums.ProjectPermissionType
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
  private val ordinalCodec = Kryo5Codec()

  @Test
  fun `writes enum constant name instead of ordinal`() {
    assertThat(codec.encodeToBytes(Scope.ADMIN).stripKryoHighBits()).contains("ADMIN")
    assertThat(ordinalCodec.encodeToBytes(Scope.ADMIN).stripKryoHighBits()).doesNotContain("ADMIN")
  }

  @Test
  fun `does not read entries written by the ordinal codec`() {
    assertThatThrownBy { codec.decode(ordinalCodec.encode(Scope.ADMIN)) }.isInstanceOf(KryoException::class.java)
    assertThatThrownBy {
      codec.decode(ordinalCodec.encode(arrayOf(Scope.ADMIN, Scope.KEYS_EDIT)))
    }.isInstanceOf(KryoException::class.java)
    assertThatThrownBy { codec.decode(ordinalCodec.encode(permissionDtoFixture)) }
      .isInstanceOf(KryoException::class.java)
    assertThatThrownBy { codec.decode(ordinalCodec.encode(apiKeyDtoWithoutExpiryFixture)) }
      .isInstanceOf(KryoException::class.java)
  }

  @Test
  fun `writes names into map keys too, since Redisson encodes them with the value encoder`() {
    val key = arrayListOf(1L, Scope.ADMIN)

    assertThat(codec.encodeKeyToBytes(key).stripKryoHighBits()).contains("ADMIN")
    assertThat(ordinalCodec.encodeKeyToBytes(key).stripKryoHighBits()).doesNotContain("ADMIN")
  }

  @Test
  fun `round trips enums standalone and nested in collections`() {
    val scopes = listOf(Scope.ADMIN, Scope.KEYS_EDIT)
    assertThat(codec.roundTrip(Scope.ADMIN)).isEqualTo(Scope.ADMIN)
    assertThat(codec.roundTrip(ArrayList(scopes))).isEqualTo(scopes)
    assertThat(codec.roundTrip(scopes.toTypedArray())).isEqualTo(scopes.toTypedArray())
  }

  @Test
  fun `round trips enum fields of a cached DTO, including a null enum`() {
    val withType = permissionDtoFixture.copy(type = ProjectPermissionType.MANAGE)

    assertThat(codec.roundTrip(permissionDtoFixture)).isEqualTo(permissionDtoFixture)
    assertThat(codec.roundTrip(withType)).isEqualTo(withType)
    assertThat(codec.roundTrip(apiKeyDtoWithoutExpiryFixture)).isEqualTo(apiKeyDtoWithoutExpiryFixture)
    assertThat(codec.encodeToBytes(permissionDtoFixture).stripKryoHighBits()).contains("ADMIN")
    assertThat(ordinalCodec.encodeToBytes(permissionDtoFixture).stripKryoHighBits()).doesNotContain("ADMIN")
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

  private fun Codec.decode(buffer: ByteBuf) = valueDecoder.decode(buffer, State())

  private fun Codec.encodeToBytes(value: Any): ByteArray = ByteBufUtil.getBytes(encode(value))

  private fun Codec.encodeKeyToBytes(value: Any): ByteArray = ByteBufUtil.getBytes(mapKeyEncoder.encode(value))

  private fun Codec.roundTrip(value: Any): Any? = decode(encode(value))
}
