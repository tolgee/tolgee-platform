import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { LanguagePermissionsMenu } from 'tg.component/security/LanguagePermissionsMenu';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { KeyFormType } from './types';

export const KeyAdvanced = () => {
  const project = useProject();
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<KeyFormType>();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page: 0,
      size: 1000,
      sort: ['tag'],
    },
  });

  if (languagesLoadable.isLoading) {
    return <BoxLoading />;
  }

  return (
    <>
      <Box>
        <Typography fontWeight="bold">
          {t('key_edit_modal_disabled_translations')}
        </Typography>

        <Typography fontSize={14}>
          {t('key_edit_modal_disabled_languages_hint')}
        </Typography>
      </Box>

      <Box mb={2}>
        <LanguagePermissionsMenu
          selected={values.disabledLangs}
          onSelect={(value) => setFieldValue('disabledLangs', value)}
          allLanguages={languagesLoadable.data?._embedded?.languages || []}
          buttonProps={{ style: { minWidth: 180 } }}
          emptyLabel={t('disabled_languages_none')}
        />
      </Box>
    </>
  );
};
