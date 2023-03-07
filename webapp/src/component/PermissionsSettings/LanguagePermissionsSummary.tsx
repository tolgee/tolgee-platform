import { useTranslate } from '@tolgee/react';
import { Tooltip } from '@mui/material';
import { LanguagesPermittedList } from 'tg.component/languages/LanguagesPermittedList';
import { LanguageModel, PermissionModel } from './types';
import { getLanguagesByRole } from './utils';
import { LanguagePermissionCategory, LanguagesHint } from './LaguagesHint';

type Props = {
  permissions: PermissionModel;
  allLangs: LanguageModel[];
};

export function LanguagePermissionSummary({ permissions, allLangs }: Props) {
  const { t } = useTranslate();
  const { viewLanguageIds, translateLanguageIds, stateChangeLanguageIds } =
    permissions;

  const categories: LanguagePermissionCategory[] = [
    {
      label: t('permission_type_review'),
      data: stateChangeLanguageIds,
      scope: 'translations.state-edit',
    },
    {
      label: t('permission_type_edit'),
      data: translateLanguageIds,
      scope: 'translations.edit',
    },
    {
      label: t('permission_type_view'),
      data: viewLanguageIds,
      scope: 'translations.view',
    },
  ];

  if (!categories.find((c) => Boolean(c.data?.length))) {
    return null;
  }

  const displayed = getLanguagesByRole(permissions);

  const isVarious = displayed === null;

  return (
    <Tooltip
      title={
        <LanguagesHint
          categories={categories}
          permissions={permissions}
          allLangs={allLangs}
        />
      }
    >
      <div>
        {isVarious ? (
          t('languages_permitted_list_various')
        ) : (
          <LanguagesPermittedList
            languages={displayed?.map(
              (langId) => allLangs.find((l) => l.id === langId)!
            )}
          />
        )}
      </div>
    </Tooltip>
  );
}
