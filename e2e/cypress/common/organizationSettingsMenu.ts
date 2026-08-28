import { gcyAdvanced } from './shared';

export type OrganizationSettingsMenuItem =
  | 'profile'
  | 'members'
  | 'member-privileges'
  | 'glossaries'
  | 'translation-memories'
  | 'apps';

export const organizationSettingsMenuItem = (
  item: OrganizationSettingsMenuItem,
  options?: Parameters<typeof gcyAdvanced>[1]
) => gcyAdvanced({ value: 'settings-menu-item', item }, options);
