import { describe, expect, it } from 'vitest';

import { organizationSettingsMenu } from 'tg.fixtures/organizationSettingsMenu';

const menu = (over: Partial<Parameters<typeof organizationSettingsMenu>[0]>) =>
  organizationSettingsMenu({
    canManage: false,
    isAtLeastOrganizationMember: false,
    llmEnabled: false,
    billingEnabled: false,
    internalControllerEnabled: false,
    ...over,
  });

describe('organizationSettingsMenu', () => {
  it('offers a viewer with no role only the profile and the glossaries', () => {
    expect(menu({})).toEqual(['profile', 'glossaries']);
  });

  it('adds translation memories for a member without hinting at management', () => {
    expect(menu({ isAtLeastOrganizationMember: true })).toEqual([
      'profile',
      'glossaries',
      'translation-memories',
    ]);
  });

  it('keeps the members pages ahead of the glossaries for a manager', () => {
    expect(menu({ canManage: true })).toEqual([
      'profile',
      'members',
      'member-privileges',
      'glossaries',
      'apps',
      'sso',
    ]);
  });

  it('withholds every manage-only page from a member, whatever the server enables', () => {
    expect(
      menu({
        isAtLeastOrganizationMember: true,
        llmEnabled: true,
        billingEnabled: true,
        internalControllerEnabled: true,
      })
    ).toEqual(['profile', 'glossaries', 'translation-memories']);
  });

  it('offers the LLM providers only where the server runs them', () => {
    expect(menu({ canManage: true, llmEnabled: true })).toContain(
      'llm-providers'
    );
    expect(menu({ canManage: true })).not.toContain('llm-providers');
  });

  it('offers the billing pages only where billing is enabled', () => {
    expect(menu({ canManage: true, billingEnabled: true })).toEqual([
      'profile',
      'members',
      'member-privileges',
      'glossaries',
      'apps',
      'sso',
      'subscriptions',
      'invoices',
    ]);
  });

  it('hides the test-clock helper unless the internal controller is on, and never without billing', () => {
    expect(
      menu({
        canManage: true,
        billingEnabled: true,
        internalControllerEnabled: true,
      })
    ).toContain('billing-test-clock');
    expect(
      menu({ canManage: true, internalControllerEnabled: true })
    ).not.toContain('billing-test-clock');
  });
});
