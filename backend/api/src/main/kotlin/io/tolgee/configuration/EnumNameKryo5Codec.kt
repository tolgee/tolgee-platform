package io.tolgee.configuration

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.EnumNameSerializer
import org.redisson.codec.Kryo5Codec

class EnumNameKryo5Codec : Kryo5Codec {
  constructor() : super()

  /**
   * Redisson reflectively looks up a `(ClassLoader, <this exact type>)` constructor to rebind the codec to the thread
   * context classloader (`RedisExecutor.getCodec`, enabled by default via `useThreadClassLoader`).
   */
  constructor(classLoader: ClassLoader?, codec: EnumNameKryo5Codec) : super(classLoader, codec)

  override fun createKryo(classLoader: ClassLoader?): Kryo {
    return super.createKryo(classLoader).apply {
      addDefaultSerializer(Enum::class.java, EnumNameSerializer::class.java)
    }
  }
}
