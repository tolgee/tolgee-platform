package io.tolgee.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YamlObjectMapperConfiguration {
  @Bean("yamlObjectMapper")
  fun yamlObjectMapper(): ObjectMapper {
    return ObjectMapper(YAMLFactory())
  }
}
