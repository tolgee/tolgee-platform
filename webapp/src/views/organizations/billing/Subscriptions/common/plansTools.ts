import { PlanType } from '../cloud/Plans/types';

export function isSubset<T>(set: T[], subset: T[]): boolean {
  return subset.every((i) => set.includes(i));
}

export function excludePreviousPlanFeatures(prevPaidPlans: PlanType[]) {
  const [currentPlan, ...previousPlans] = [...prevPaidPlans].reverse();
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
