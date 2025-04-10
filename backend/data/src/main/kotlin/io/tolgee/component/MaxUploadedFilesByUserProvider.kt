package io.tolgee.component

import org.springframework.stereotype.Component

@Component
class MaxUploadedFilesByUserProvider {
  operator fun invoke(): Long = 100L
}
