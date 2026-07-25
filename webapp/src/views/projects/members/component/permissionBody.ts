import {
  LanguagePermissions,
  PermissionModelRole,
} from 'tg.component/PermissionsSettings/types';

export function languagePermissionsForRole(
  role: PermissionModelRole,
  languages: number[] | undefined
) {
  let languagePermissions: LanguagePermissions = {};
  if (role === 'REVIEW') {
    languagePermissions = {
      translateLanguages: languages,
      stateChangeLanguages: languages,
      suggestLanguages: languages,
    };
  } else if (role === 'TRANSLATE') {
    languagePermissions = {
      translateLanguages: languages,
      suggestLanguages: languages,
    };
  }
  return languagePermissions;
}
