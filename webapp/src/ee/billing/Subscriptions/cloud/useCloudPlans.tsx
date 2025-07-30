import { excludePreviousPlanFeatures } from '../../component/Plan/plansTools';
import { PlanType } from '../../component/Plan/types';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const useCloudPlans = () => {
  const organization = useOrganization();

  const plansLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/plans`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const serverPlans = plansLoadable?.data?._embedded?.plans;

  if (!serverPlans) {
    return {
      plansLoadable: plansLoadable,
    };
  }

  const defaultPlan = serverPlans.find((p) => p.free && p.public);
  const publicPlans = serverPlans.filter(
    (p) => p !== defaultPlan && p.public
  ) as PlanType[];
  const customPlans = serverPlans.filter((p) => !p.public);

  // add enterprise plan
  publicPlans.push({
    id: -1,
    type: 'CONTACT_US',
    name: 'Enterprise',
    enabledFeatures: [
      'ACCOUNT_MANAGER',
      'DEDICATED_SLACK_CHANNEL',
      'SLACK_INTEGRATION',
      'GRANULAR_PERMISSIONS',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'PREMIUM_SUPPORT',
      'PRIORITIZED_FEATURE_REQUESTS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'STANDARD_SUPPORT',
      'WEBHOOKS',
      'TASKS',
      'ORDER_TRANSLATION',
      'SSO',
      'AI_PROMPT_CUSTOMIZATION',
      'GLOSSARY',
      'TRANSLATION_LABELS',
    ] as const satisfies PlanType['enabledFeatures'],
    free: false,
    hasYearlyPrice: false,
    public: true,
    nonCommercial: false,
    metricType: 'KEYS_SEATS',
    includedUsage: {
      seats: -2,
      keys: -2,
      mtCredits: -2,
      translations: -2,
    },
    archivedAt: undefined,
  });

  const parentForPublic: PlanType[] = [];
  const parentForCustom: PlanType[] = [];
  if (defaultPlan) {
    parentForPublic.push(defaultPlan);
    parentForCustom.push(defaultPlan);
  }
  publicPlans.forEach((p) => parentForCustom.push(p));

  const combinedPlans = [
    ...customPlans.map((plan) => ({
      plan,
      custom: true,
      ...excludePreviousPlanFeatures(plan, parentForCustom),
    })),
    ...publicPlans.map((plan) => {
      const featuresInfo = excludePreviousPlanFeatures(plan, parentForPublic);
      parentForPublic.push(plan);
      return {
        plan,
        custom: false,
        ...featuresInfo,
      };
    }),
  ];

  return {
    plans: combinedPlans,
    defaultPlan,
    plansLoadable,
  };
};

export type PlanInfoType = NonNullable<
  ReturnType<typeof useCloudPlans>['plans']
>[0];
