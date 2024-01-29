package io.tolgee.formats.po.out.php

import io.tolgee.service.export.exporters.ConversionResult

interface ToPoMessageConverter {
  fun convert(): ConversionResult
}
