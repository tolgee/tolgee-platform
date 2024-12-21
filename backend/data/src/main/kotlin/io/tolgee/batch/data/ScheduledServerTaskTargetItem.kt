package io.tolgee.batch.data

import java.util.*

data class ScheduledServerTaskTargetItem(val jobBean: String, val executeAfter: Date, val data: Any)
