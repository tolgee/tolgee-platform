package io.tolgee.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLWriteFeature

@Configuration
class YamlObjectMapperConfiguration {
  @Bean("yamlObjectMapper")
  fun yamlObjectMapper(): ObjectMapper {
    val factory =
      YAMLFactory
        .builder()
        .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
        .disable(YAMLWriteFeature.SPLIT_LINES)
        .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
        .build()
    return ObjectMapper(factory)
  }
}
