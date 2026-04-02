import React, { useCallback } from 'react';
import { Box, Typography, Switch, styled } from '@mui/material';
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

const StyledContainer = styled(Box)`
  margin-top: ${({ theme }) => theme.spacing(3)};
`;

const StyledTitleRow = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  margin-top: ${({ theme }) => theme.spacing(3)};
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

const StyledDescription = styled(Typography)`
  margin-bottom: ${({ theme }) => theme.spacing(3)};
`;

const StyledSettingsBody = styled(Box)`
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
      <StyledContainer>
        <DisabledFeatureBanner
          customMessage={t('qa_checks_feature_description')}
        />
      </StyledContainer>
    );
  }

  if (!settings.data) {
    return null;
  }

  const disabled = !qaProjectEnabled;

  return (
    <StyledContainer>
      <StyledTitleRow>
        <Typography variant="h5">
          <T keyName="project_settings_qa_global_title" />
        </Typography>
        <Switch
          checked={qaProjectEnabled}
          onChange={toggleQaMutation.isLoading ? undefined : handleToggle}
          size="small"
          data-cy="qa-enabled-toggle"
        />
      </StyledTitleRow>
      <StyledDescription variant="body2" color="text.secondary">
        <T keyName="project_settings_qa_global_description" />
      </StyledDescription>

      <StyledSettingsBody sx={{ opacity: disabled ? 0.5 : 1 }}>
        {Object.keys(settings.data?.settings || {}).map((type) => (
          <QaSettingsItem
            key={type}
            type={type as QaCheckType}
            value={settings.data?.settings?.[type as QaCheckType] || 'OFF'}
            onChange={handleChange}
            disabled={disabled}
          />
        ))}

        {settings.data?.settings && (
          <QaLanguageSettings
            globalSettings={
              settings.data.settings as Record<QaCheckType, QaCheckSeverity>
            }
            disabled={disabled}
          />
        )}
      </StyledSettingsBody>
    </StyledContainer>
  );
};
