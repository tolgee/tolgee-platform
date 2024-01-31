package io.tolgee.formats.po.out

import io.tolgee.service.export.exporters.ConversionResult

interface ToPoMessageConverter {
  fun convert(): ConversionResult
}
