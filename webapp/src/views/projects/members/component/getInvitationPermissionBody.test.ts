import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { getInvitationPermissionBody } from './getInvitationPermissionBody';

const state = (
  partial: Partial<PermissionSettingsState>
): PermissionSettingsState => ({
  tab: 'basic',
  advancedState: { scopes: [] },
  basicState: {},
  ...partial,
});

describe('getInvitationPermissionBody', () => {
  it('maps a basic role to a type body', () => {
    expect(
      getInvitationPermissionBody(
        state({ tab: 'basic', basicState: { role: 'TRANSLATE' } })
      )
    ).toEqual({ ok: true, body: { type: 'TRANSLATE' } });
  });

  it('omits language fields for a role that carries none (VIEW/MANAGE)', () => {
    expect(
      getInvitationPermissionBody(
        state({ tab: 'basic', basicState: { role: 'VIEW', languages: [7] } })
      )
    ).toEqual({ ok: true, body: { type: 'VIEW' } });
  });

  it('maps a basic TRANSLATE role with languages (no stateChangeLanguages)', () => {
    expect(
      getInvitationPermissionBody(
        state({
          tab: 'basic',
          basicState: { role: 'TRANSLATE', languages: [7] },
        })
      )
    ).toEqual({
      ok: true,
      body: {
        type: 'TRANSLATE',
        translateLanguages: [7],
        suggestLanguages: [7],
      },
    });
  });

  it('maps a basic REVIEW role with languages including stateChangeLanguages', () => {
    expect(
      getInvitationPermissionBody(
        state({ tab: 'basic', basicState: { role: 'REVIEW', languages: [7] } })
      )
    ).toEqual({
      ok: true,
      body: {
        type: 'REVIEW',
        translateLanguages: [7],
        stateChangeLanguages: [7],
        suggestLanguages: [7],
      },
    });
  });

  it('maps advanced scopes with language permissions', () => {
    expect(
      getInvitationPermissionBody(
        state({
          tab: 'advanced',
          advancedState: {
            scopes: ['translations.edit'],
            translateLanguages: [7],
          },
        })
      )
    ).toEqual({
      ok: true,
      body: { scopes: ['translations.edit'], translateLanguages: [7] },
    });
  });

  it('drops language fields not applicable to the selected scopes', () => {
    expect(
      getInvitationPermissionBody(
        state({
          tab: 'advanced',
          advancedState: {
            scopes: ['translations.edit'],
            translateLanguages: [7],
            stateChangeLanguages: [9],
          },
        })
      )
    ).toEqual({
      ok: true,
      body: { scopes: ['translations.edit'], translateLanguages: [7] },
    });
  });

  it('reports empty-scopes for advanced with no scopes', () => {
    expect(
      getInvitationPermissionBody(
        state({ tab: 'advanced', advancedState: { scopes: [] } })
      )
    ).toEqual({ ok: false, reason: 'empty-scopes' });
  });

  it('reports invalid for basic with no role', () => {
    expect(
      getInvitationPermissionBody(state({ tab: 'basic', basicState: {} }))
    ).toEqual({ ok: false, reason: 'invalid' });
  });
});
