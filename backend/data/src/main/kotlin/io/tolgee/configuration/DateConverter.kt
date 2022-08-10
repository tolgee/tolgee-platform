package io.tolgee.configuration

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*

@Component
@ConfigurationPropertiesBinding
class DateConverter : Converter<String?, Date?> {
  override fun convert(source: String?): Date? {
    return if (source == null) {
      null
    } else SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(source)
  }
}
