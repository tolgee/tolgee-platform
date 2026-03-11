import { useCallback } from 'react';
import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { QaSettingsItem } from 'tg.ee.module/qa/components/QaSettingsItem';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

export const ProjectSettingsQa = () => {
  const project = useProject();

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  const saveSettingsMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'put',
  });

  const handleChange = useCallback(
    (checkType: string, severity: QaCheckSeverity) => {
      saveSettingsMutation.mutate({
        path: { projectId: project.id },
        content: {
          'application/json': {
            settings: { [checkType]: severity },
          },
        },
      });
    },
    [project.id]
  );

  if (!settings) {
    return null;
  }

  return (
    <Box sx={{ mt: 3 }}>
      <Typography variant="h5" sx={{ mb: 1 }}>
        <T keyName="project_settings_qa_global_title" />
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        <T keyName="project_settings_qa_global_description" />
      </Typography>

      {Object.keys(settings.data || {}).map((type) => (
        <QaSettingsItem
          key={type}
          type={type as QaCheckType}
          value={settings.data?.[type as QaCheckType] || 'OFF'}
          onChange={handleChange}
        />
      ))}
    </Box>
  );
};
