import { PlanType } from './types';

export function isSubset<T>(set: T[], subset: T[]): boolean {
  return subset.every((i) => set.includes(i));
}

export function excludePreviousPlanFeatures(
  currentPlan: PlanType,
  prevPlans: PlanType[]
) {
  const previousPlans = [...prevPlans].reverse();

  const parentPlan = previousPlans.find(
    (plan) =>
      plan.public && isSubset(currentPlan.enabledFeatures, plan.enabledFeatures)
  );

  if (parentPlan) {
    return {
      filteredFeatures: currentPlan.enabledFeatures.filter(
        (i) => !parentPlan.enabledFeatures.includes(i)
      ),
      previousPlanName: parentPlan.name,
    };
  } else {
    return {
      filteredFeatures: currentPlan.enabledFeatures,
    };
  }
}

export function isPlanLegacy(plan: PlanType) {
  const slots = plan.includedUsage?.translationSlots;
  return slots !== undefined && slots !== -1;
}

export function isPlanPeriodDependant(prices: PlanType['prices'] | undefined) {
  return (
    prices && Boolean(prices.subscriptionYearly || prices.subscriptionMonthly)
  );
}
