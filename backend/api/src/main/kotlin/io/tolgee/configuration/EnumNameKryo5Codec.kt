package io.tolgee.configuration

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.EnumNameSerializer
import org.redisson.codec.Kryo5Codec

/**
 * Kryo's default enum serializer writes an enum's ordinal, so reordering or removing a constant makes
 * already-cached entries decode as a different constant, without any error. Serializing by name instead
 * keeps cached values bound to the constant they were written as.
 */
class EnumNameKryo5Codec : Kryo5Codec {
  constructor() : super()

  constructor(classLoader: ClassLoader?) : super(classLoader)

  /** Invoked reflectively by `BaseCodec.copy`, which looks up a `(ClassLoader, <this exact type>)` constructor. */
  constructor(classLoader: ClassLoader?, codec: EnumNameKryo5Codec) : super(classLoader, codec)

  override fun createKryo(classLoader: ClassLoader?): Kryo {
    return super.createKryo(classLoader).apply {
      addDefaultSerializer(Enum::class.java, EnumNameSerializer::class.java)
    }
  }
}
