package io.tolgee.batch

import org.springframework.amqp.core.Message
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ChunkProcessingUtilFactory {
  fun process(message: Message, applicationContext: ApplicationContext): ChunkProcessingUtil {
    return ChunkProcessingUtil(message, applicationContext)
  }
}
