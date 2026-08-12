import { describe, expect, it } from 'vitest';
import { groupConsentScopes } from './consentScopeGroups';
import { HierarchyType } from 'tg.component/PermissionsSettings/types';

const structure: HierarchyType = {
  value: 'admin',
  children: [
    {
      label: 'Keys',
      children: [{ value: 'keys.view' }, { value: 'keys.edit' }],
    },
    {
      label: 'Translations',
      children: [
        { value: 'translations.view' },
        { value: 'translations.edit' },
        { value: 'translations.state-edit' },
      ],
    },
    { value: 'project.edit' },
  ],
};

describe('groupConsentScopes', () => {
  it('groups scopes under their resource label, preserving order', () => {
    expect(
      groupConsentScopes(
        ['keys.view', 'keys.edit', 'translations.view', 'translations.edit'],
        structure
      )
    ).toEqual([
      { label: 'Keys', scopes: ['keys.view', 'keys.edit'] },
      {
        label: 'Translations',
        scopes: ['translations.view', 'translations.edit'],
      },
    ]);
  });

  it('collects scopes with no labeled ancestor into an unlabeled group', () => {
    expect(groupConsentScopes(['project.edit'], structure)).toEqual([
      { label: undefined, scopes: ['project.edit'] },
    ]);
  });

  it('keeps a group per resource in first-seen order', () => {
    expect(
      groupConsentScopes(['translations.view', 'keys.view'], structure)
    ).toEqual([
      { label: 'Translations', scopes: ['translations.view'] },
      { label: 'Keys', scopes: ['keys.view'] },
    ]);
  });
});
