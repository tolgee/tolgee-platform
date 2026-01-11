package io.tolgee.service.export.exporters

import java.io.InputStream

interface FileExporter {
  fun produceFiles(): Map<String, InputStream>
}
