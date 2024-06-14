import { PlanType } from './types';

export function isPlanLegacy(plan: PlanType) {
  const slots = plan.includedUsage?.translationSlots;
  return slots !== undefined && slots !== -1;
}
