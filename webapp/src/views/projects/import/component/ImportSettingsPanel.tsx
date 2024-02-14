import React, { FC } from 'react';
import { Box } from '@mui/material';
import { useImportDataHelper } from '../hooks/useImportDataHelper';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LoadingCheckboxWithSkeleton } from 'tg.component/common/form/LoadingCheckboxWithSkeleton';

type ImportSettingRequest = components['schemas']['ImportSettingsRequest'];

export const ImportSettingsPanel: FC = (props) => {
  const { result } = useImportDataHelper();
  const project = useProject();
  const { t } = useTranslate();

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/import-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  return (
    <Box>
      <LoadingCheckboxWithSkeleton
        loading={true}
        hint={t('import_convert_placeholders_to_icu_checkbox_label_hint')}
        label={t('import_convert_placeholders_to_icu_checkbox_label')}
        checked={settings?.data?.convertPlaceholdersToIcu}
      />
    </Box>
  );
};
