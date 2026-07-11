package io.tolgee.util

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

val Pageable.withoutSort: Pageable
  get() {
    return PageRequest.of(this.pageNumber, this.pageSize)
  }
