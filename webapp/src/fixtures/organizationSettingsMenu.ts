export type OrganizationMenuItemId =
  | 'profile'
  | 'members'
  | 'member-privileges'
  | 'glossaries'
  | 'translation-memories'
  | 'apps'
  | 'llm-providers'
  | 'sso'
  | 'subscriptions'
  | 'invoices'
  | 'billing-test-clock';

type Params = {
  canManage: boolean;
  /** Role half only — SharedTranslationMemoryController also gates on Feature.TRANSLATION_MEMORY. */
  isAtLeastOrganizationMember: boolean;
  llmEnabled: boolean;
  billingEnabled: boolean;
  internalControllerEnabled: boolean;
};

export const organizationSettingsMenu = ({
  canManage,
  isAtLeastOrganizationMember,
  llmEnabled,
  billingEnabled,
  internalControllerEnabled,
}: Params): OrganizationMenuItemId[] => {
  const items: OrganizationMenuItemId[] = ['profile'];

  if (canManage) {
    items.push('members', 'member-privileges');
  }

  items.push('glossaries');

  if (isAtLeastOrganizationMember) {
    items.push('translation-memories');
  }

  if (!canManage) {
    return items;
  }

  items.push('apps');

  if (llmEnabled) {
    items.push('llm-providers');
  }

  items.push('sso');

  if (billingEnabled) {
    items.push('subscriptions', 'invoices');

    if (internalControllerEnabled) {
      items.push('billing-test-clock');
    }
  }

  return items;
};
