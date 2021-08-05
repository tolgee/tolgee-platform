package io.tolgee.component

import org.springframework.stereotype.Component
import java.util.*

@Component
class CurrentDateProvider {
  fun getDate(): Date {
    return Date()
  }
}
