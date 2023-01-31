import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  PermissionAdvanced,
  PermissionModelScope,
} from 'tg.component/PermissionsSettings/types';
import { Hierarchy } from './Hierarchy';
import { useTranslate } from '@tolgee/react';

type Props = {
  state: PermissionAdvanced;
  onChange: (value: PermissionAdvanced) => void;
};

export const PermissionsAdvanced: React.FC<Props> = ({ state, onChange }) => {
  const { t } = useTranslate();

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  const setScopes = (scopes: PermissionModelScope[], value: boolean) => {
    let newScopes = [...state.scopes];
    scopes.forEach((scope) => {
      const exists = newScopes.includes(scope);
      if (exists && value === false) {
        newScopes = newScopes.filter((s) => s !== scope);
      } else if (!exists && value === true) {
        newScopes = [...newScopes, scope];
      }
    });
    onChange({
      ...state,
      scopes: newScopes,
    });
  };

  if (dependenciesLoadable.isLoading) {
    return <FullPageLoading />;
  }

  if (!dependenciesLoadable.data) {
    return null;
  }

  return (
    <Hierarchy
      dependencies={dependenciesLoadable.data}
      setScopes={setScopes}
      scopes={state.scopes}
      structure={{
        label: t('permissions_item_admin'),
        value: 'admin',
        children: [
          {
            label: t('permissions_item_translations'),
            children: [
              {
                label: t('permissions_item_translations_view'),
                value: 'translations.view',
              },
              {
                label: t('permissions_item_translations_edit'),
                value: 'translations.edit',
              },
              {
                label: t('permissions_item_translations_comments'),
                children: [
                  {
                    label: t('permissions_item_translations_comments_add'),
                    value: 'translation-comments.add',
                  },
                  {
                    label: t('permissions_item_translations_comments_edit'),
                    value: 'translation-comments.edit',
                  },
                  {
                    label: t(
                      'permissions_item_translations_comments_set_state'
                    ),
                    value: 'translation-comments.set-state',
                  },
                ],
              },
              {
                label: t('permissions_item_translations_state'),
                value: 'translations.state-edit',
              },
            ],
          },
          {
            label: t('permissions_item_screenshots'),
            children: [
              {
                label: t('permissions_item_screenshots_view'),
                value: 'screenshots.view',
              },
              {
                label: t('permissions_item_screenshots_upload'),
                value: 'screenshots.upload',
              },
              {
                label: t('permissions_item_screenshots_delete'),
                value: 'screenshots.delete',
              },
            ],
          },
          {
            label: t('permissions_item_keys_edit'),
            value: 'keys.edit',
          },
          {
            label: t('permissions_item_import'),
            value: 'import',
          },
        ],
      }}
    />
  );
};
