package io.tolgee.development

import org.springframework.context.ApplicationEvent
import java.util.*

class OnDateForced(source: Any, val value: Date?) : ApplicationEvent(source)
