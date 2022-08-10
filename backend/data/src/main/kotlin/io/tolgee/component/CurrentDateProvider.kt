package io.tolgee.component

import org.springframework.stereotype.Component
import java.util.*

@Component
class CurrentDateProvider {
  val date: Date
    get() {
      return Date()
    }
}
