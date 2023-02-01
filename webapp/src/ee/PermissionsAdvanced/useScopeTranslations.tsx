import { useTranslate } from '@tolgee/react';
import { PermissionModelScope } from 'tg.component/PermissionsSettings/types';

export const useScopeTranslations = () => {
  const { t } = useTranslate();

  const labels: Record<PermissionModelScope, string> = {
    admin: t('permissions_item_admin'),
    'translations.view': t('permissions_item_translations_view'),
    'translations.edit': t('permissions_item_translations_edit'),
    'translation-comments.add': t('permissions_item_translations_comments_add'),
    'translation-comments.edit': t(
      'permissions_item_translations_comments_edit'
    ),
    'translation-comments.set-state': t(
      'permissions_item_translations_comments_set_state'
    ),
    'translations.state-edit': t('permissions_item_translations_state'),
    'screenshots.view': t('permissions_item_screenshots_view'),
    'screenshots.upload': t('permissions_item_screenshots_upload'),
    'screenshots.delete': t('permissions_item_screenshots_delete'),
    'keys.edit': t('permissions_item_keys_edit'),
    'project.edit': t('permissions_item_project_edit'),
    'users.view': t('permission_item_users_view'),
    'languages.edit': t('permissions_item_languages_edit'),
    'activity.view': t('permissions_item_activity_view'),
    import: t('permissions_item_import'),
  };

  return {
    getScopeTranslation: (scope: PermissionModelScope) => labels[scope],
  };
};
