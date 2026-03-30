/**
 * Stub billing module — used when the billing repo is not present.
 * When the billing repo exists, Vite's alias overrides this with
 * the real implementation from billing/frontend/billing/billingModule.tsx.
 */
import { Link } from 'tg.constants/links';
import { BillingMenuItemsProps } from 'eeSetup/EeModuleType';

const Empty = () => null;

export type BillingAdministrationMenuItem = {
  id: string;
  link: Link;
  label: string;
};

export const billingModule = {
  GlobalLimitPopover: Empty,
  CriticalUsageCircle: Empty as React.FC<{ sx?: any }>,
  TrialAnnouncement: Empty,
  TrialChip: Empty,

  billingMenuItems: [] as React.FC<BillingMenuItemsProps>[],

  AdministrationRoutes: Empty,
  OrganizationRoutes: Empty,

  useAdministrationMenuItems: (): BillingAdministrationMenuItem[] => [],
};
