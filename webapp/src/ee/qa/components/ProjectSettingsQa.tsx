import { useCallback } from 'react';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { QaSettingsItem } from 'tg.ee.module/qa/components/QaSettingsItem';
import { QaLanguageSettings } from './QaLanguageSettings';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

export const ProjectSettingsQa = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { isEnabled } = useEnabledFeatures();
  const qaEnabled = isEnabled('QA_CHECKS');

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'get',
    path: { projectId: project.id },
    options: { enabled: qaEnabled },
  });

  const saveSettingsMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-settings',
  });

  const handleChange = useCallback(
    (checkType: QaCheckType, severity: QaCheckSeverity) => {
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

  if (!qaEnabled) {
    return (
      <Box sx={{ mt: 3 }}>
        <DisabledFeatureBanner
          customMessage={t('qa_checks_feature_description')}
        />
      </Box>
    );
  }

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

      {Object.keys(settings.data?.settings || {}).map((type) => (
        <QaSettingsItem
          key={type}
          type={type as QaCheckType}
          value={settings.data?.settings?.[type as QaCheckType] || 'OFF'}
          onChange={handleChange}
        />
      ))}

      {settings.data?.settings && (
        <QaLanguageSettings
          globalSettings={
            settings.data.settings as Record<QaCheckType, QaCheckSeverity>
          }
        />
      )}
    </Box>
  );
};
