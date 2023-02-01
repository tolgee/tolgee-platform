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
        value: 'admin',
        children: [
          {
            label: t('permissions_item_translations'),
            children: [
              {
                value: 'translations.view',
              },
              {
                value: 'translations.edit',
              },
              {
                label: t('permissions_item_translations_comments'),
                children: [
                  {
                    value: 'translation-comments.add',
                  },
                  {
                    value: 'translation-comments.edit',
                  },
                  {
                    value: 'translation-comments.set-state',
                  },
                ],
              },
              {
                value: 'translations.state-edit',
              },
            ],
          },
          {
            label: t('permissions_item_screenshots'),
            children: [
              {
                value: 'screenshots.view',
              },
              {
                value: 'screenshots.upload',
              },
              {
                value: 'screenshots.delete',
              },
            ],
          },
          {
            value: 'keys.edit',
          },
          {
            value: 'users.view',
          },
          {
            value: 'project.edit',
          },
          {
            value: 'activity.view',
          },
          {
            value: 'languages.edit',
          },
          {
            value: 'import',
          },
        ],
      }}
    />
  );
};
