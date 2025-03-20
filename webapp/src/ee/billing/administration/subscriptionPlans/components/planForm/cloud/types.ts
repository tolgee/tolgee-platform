import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanModel = components['schemas']['CloudPlanRequest'];

type EnabledFeature =
  components['schemas']['CloudPlanRequest']['enabledFeatures'][number];

export type MetricType =
  components['schemas']['CloudPlanRequest']['metricType'];

export interface GenericPlanFormData {
  name: string;
  prices: CloudPlanModel['prices'];
  includedUsage: CloudPlanModel['includedUsage'];
  stripeProductId?: string;
  enabledFeatures: EnabledFeature[];
  forOrganizationIds: number[];
  public: boolean;
  free: boolean;
  nonCommercial: boolean;
}

export interface CloudPlanFormData extends GenericPlanFormData {
  type: CloudPlanModel['type'];
  metricType: MetricType;
}

export type SelfHostedEePlanFormData =
  components['schemas']['SelfHostedEePlanRequest'];
