import { useCallback } from 'react';
import { Box, Typography, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { QaSettingsItem } from 'tg.ee.module/qa/components/QaSettingsItem';
import { QaLanguageSettings } from './QaLanguageSettings';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { SwitchWithDescription } from 'tg.views/projects/project/components/SwitchWithDescription';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

const StyledSettingsBody = styled('div')`
  transition: opacity 0.2s;
`;

export const ProjectSettingsQa = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { isEnabled } = useEnabledFeatures();
  const qaFeatureEnabled = isEnabled('QA_CHECKS');
  const qaProjectEnabled = project.useQaChecks;

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'get',
    path: { projectId: project.id },
    options: { enabled: qaFeatureEnabled },
  });

  const saveSettingsMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-settings',
  });

  const toggleQaMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings/enabled',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const handleToggle = useCallback(() => {
    toggleQaMutation.mutate({
      path: { projectId: project.id },
      content: {
        'application/json': {
          enabled: !qaProjectEnabled,
        },
      },
    });
  }, [project.id, qaProjectEnabled]);

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

  if (!qaFeatureEnabled) {
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
      <SwitchWithDescription
        title={
          <T
            keyName="project_settings_qa_enabled_toggle"
          />
        }
        description={
          <T
            keyName="project_settings_qa_enabled_description"
          />
        }
        checked={qaProjectEnabled}
        onSwitch={handleToggle}
        disabled={toggleQaMutation.isLoading}
        data-cy="qa-enabled-toggle"
      />

      <StyledSettingsBody
        sx={{
          opacity: qaProjectEnabled ? 1 : 0.5,
          pointerEvents: qaProjectEnabled ? 'auto' : 'none',
        }}
      >
        <Typography variant="h5" sx={{ mt: 3, mb: 1 }}>
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
      </StyledSettingsBody>
    </Box>
  );
};
