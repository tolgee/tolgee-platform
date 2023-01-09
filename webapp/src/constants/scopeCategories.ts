import { expandScope, Scope } from 'tg.component/permissions/permissionHelper';

export function getScopeCategories(t: (key: string) => string) {
  const scopeCategories = [
    {
      name: t('scope_category_project_management'),
      scopes: [
        'admin',
        'users.view',
        'project.edit',
        'languages.edit',
      ] as Scope[],
    },
    {
      name: t('scope_category_translations'),
      scopes: [
        'keys.edit',
        'translations.view',
        'translations.edit',
        'translations.state-edit',
      ] as Scope[],
    },
    {
      name: t('scope_category_screenshots'),
      scopes: [
        'screenshots.view',
        'screenshots.upload',
        'screenshots.delete',
      ] as Scope[],
    },
    {
      name: t('scope_category_translation_comments'),
      scopes: [
        'translation-comments.add',
        'translation-comments.edit',
        'translation-comments.set-state',
      ] as Scope[],
    },
  ];

  const usedScopes = scopeCategories.flatMap((c) => c.scopes);
  const allScopes = expandScope('admin');

  scopeCategories.push({
    name: t('scope_category_other'),
    scopes: allScopes.filter((s) => !usedScopes.includes(s)),
  });

  return scopeCategories;
}
