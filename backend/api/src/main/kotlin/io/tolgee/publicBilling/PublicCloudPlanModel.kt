package io.tolgee.publicBilling

import io.tolgee.constants.Feature
import io.tolgee.hateoas.ee.PlanIncludedUsageModel
import java.util.Date

interface PublicCloudPlanModel {
  val id: Long
  val name: String
  val free: Boolean
  val enabledFeatures: Array<Feature>
  val type: CloudSubscriptionPlanType
  val public: Boolean
  val nonCommercial: Boolean
  val includedUsage: PlanIncludedUsageModel
  val metricType: MetricType
  val archivedAt: Date?
}
