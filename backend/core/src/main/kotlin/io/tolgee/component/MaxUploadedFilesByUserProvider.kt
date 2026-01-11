package io.tolgee.component

import org.springframework.stereotype.Component

@Component
class MaxUploadedFilesByUserProvider {
  operator fun invoke(): Long {
    return 100L
  }
}
