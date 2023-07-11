package io.tolgee.activity

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class UtmDataHolder {
  var data: Map<String, Any?>? = null
}
