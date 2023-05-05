import { useTranslate } from '@tolgee/react';
import { Tooltip, Box } from '@mui/material';
import { LanguagesPermittedList } from 'tg.component/languages/LanguagesPermittedList';
import { LanguageModel, PermissionModel } from './types';
import { LanguagePermissionCategory, LanguagesHint } from './LaguagesHint';
import { useMemo } from 'react';

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

  const languagesUnion = useMemo(() => {
    return Array.from(
      new Set([
        ...(viewLanguageIds || []),
        ...(translateLanguageIds || []),
        ...(stateChangeLanguageIds || []),
      ])
    );
  }, [viewLanguageIds, translateLanguageIds, stateChangeLanguageIds]);

  return (
    <Tooltip
      title={
        <LanguagesHint
          categories={categories}
          permissions={permissions}
          allLangs={allLangs}
        />
      }
      disableInteractive
      open={languagesUnion.length ? undefined : false}
    >
      <Box display="flex" alignItems="center">
        <LanguagesPermittedList
          languages={languagesUnion?.map(
            (langId) => allLangs.find((l) => l.id === langId)!
          )}
        />
      </Box>
    </Tooltip>
  );
}
