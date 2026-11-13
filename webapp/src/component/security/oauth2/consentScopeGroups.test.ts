import {
  groupConsentScopes,
  showsAsOneChip,
} from 'tg.component/security/oauth2/consentScopeGroups';
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
      { label: 'Keys', scopes: ['keys.view', 'keys.edit'], complete: true },
      {
        label: 'Translations',
        scopes: ['translations.view', 'translations.edit'],
        complete: false,
      },
    ]);
  });

  it('is complete only when the resource has nothing left to grant', () => {
    expect(
      groupConsentScopes(
        ['translations.view', 'translations.edit', 'translations.state-edit'],
        structure
      )
    ).toEqual([
      {
        label: 'Translations',
        scopes: [
          'translations.view',
          'translations.edit',
          'translations.state-edit',
        ],
        complete: true,
      },
    ]);
  });

  it("is not complete when the client never asked for one of the resource's scopes", () => {
    expect(groupConsentScopes(['keys.view'], structure)[0].complete).toBe(
      false
    );
  });

  it('collects scopes with no labeled ancestor into an unlabeled group', () => {
    expect(groupConsentScopes(['project.edit'], structure)).toEqual([
      { label: undefined, scopes: ['project.edit'], complete: false },
    ]);
  });

  it('lists the scopes of a group in permission tree order, not in the order asked for', () => {
    expect(
      groupConsentScopes(
        ['translations.state-edit', 'translations.view'],
        structure
      )
    ).toEqual([
      {
        label: 'Translations',
        scopes: ['translations.view', 'translations.state-edit'],
        complete: false,
      },
    ]);
  });

  it('puts the unlabeled group first, so it does not split the named ones', () => {
    expect(
      groupConsentScopes(['project.edit', 'keys.view'], structure).map(
        (group) => group.label
      )
    ).toEqual([undefined, 'Keys']);
  });

  it('answers with the root scope alone, because it already grants the rest', () => {
    expect(
      groupConsentScopes(['keys.view', 'admin', 'translations.edit'], structure)
    ).toEqual([{ label: undefined, scopes: ['admin'], complete: true }]);
  });

  it('keeps every group when the root scope was not granted', () => {
    expect(
      groupConsentScopes(['keys.view', 'translations.edit'], structure).map(
        (group) => group.label
      )
    ).toEqual(['Keys', 'Translations']);
  });

  it('keeps a group per resource in first-seen order', () => {
    expect(
      groupConsentScopes(['translations.view', 'keys.view'], structure).map(
        (group) => group.label
      )
    ).toEqual(['Translations', 'Keys']);
  });
});

describe('showsAsOneChip', () => {
  it('collapses a named group that holds its whole resource', () => {
    expect(
      showsAsOneChip({
        label: 'Keys',
        scopes: ['keys.view', 'keys.edit'],
        complete: true,
      })
    ).toBe(true);
  });

  it('keeps the names when the resource has something left to grant', () => {
    expect(
      showsAsOneChip({
        label: 'Keys',
        scopes: ['keys.view'],
        complete: false,
      })
    ).toBe(false);
  });

  it('keeps the name of a single scope, which says more than "all" would', () => {
    expect(
      showsAsOneChip({
        label: 'Keys',
        scopes: ['keys.view'],
        complete: true,
      })
    ).toBe(false);
  });

  it('never collapses the group that has no resource to name', () => {
    expect(
      showsAsOneChip({
        label: undefined,
        scopes: ['project.edit', 'admin'],
        complete: true,
      })
    ).toBe(false);
  });
});
