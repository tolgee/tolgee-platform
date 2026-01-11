package io.tolgee.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YamlObjectMapperConfiguration {
  @Bean("yamlObjectMapper")
  fun yamlObjectMapper(): ObjectMapper {
    val factory =
      YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .disable(YAMLGenerator.Feature.SPLIT_LINES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
    return ObjectMapper(factory)
  }
}
