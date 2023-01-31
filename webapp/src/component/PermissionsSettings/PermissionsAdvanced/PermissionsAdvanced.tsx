import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PermissionAdvanced, PermissionModelScope } from '../types';
import { Hierarchy } from './Hierarchy';

type Props = {
  state: PermissionAdvanced;
  onChange: (value: PermissionAdvanced) => void;
};

export const PermissionsAdvanced: React.FC<Props> = ({ state, onChange }) => {
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
        label: 'admin',
        value: 'admin',
        children: [
          {
            label: 'translations',
            children: [
              {
                label: 'view',
                value: 'translations.view',
              },
              {
                label: 'edit',
                value: 'translations.edit',
              },
              {
                label: 'comments',
                children: [
                  {
                    label: 'add',
                    value: 'translation-comments.add',
                  },
                  {
                    label: 'edit',
                    value: 'translation-comments.edit',
                  },
                  {
                    label: 'set state',
                    value: 'translation-comments.set-state',
                  },
                ],
              },
              {
                label: 'state',
                value: 'translations.state-edit',
              },
            ],
          },
          {
            label: 'screenshots',
            children: [
              {
                label: 'view',
                value: 'screenshots.view',
              },
              {
                label: 'upload',
                value: 'screenshots.upload',
              },
              {
                label: 'delete',
                value: 'screenshots.delete',
              },
            ],
          },
          {
            label: 'keys',
            value: 'keys.edit',
          },
          {
            label: 'import',
            value: 'import',
          },
        ],
      }}
    />
  );
};
