import { useState } from 'react';
import { Box, IconButton, Switch, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Settings01 } from '@untitled-ui/icons-react';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LanguageItem } from 'tg.component/languages/LanguageItem';
import {
  TABLE_DIVIDER,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
} from 'tg.component/languages/tableStyles';
import { QaLanguageSettingsDialog } from './QaLanguageSettingsDialog';
import { LanguageQaConfigModel } from 'tg.service/apiSchemaTypes.generated';
import { QaCheckType, QaCheckSeverity } from 'tg.service/apiSchemaTypes';

type Props = {
  languageConfig: LanguageQaConfigModel;
  globalSettings: Record<QaCheckType, QaCheckSeverity>;
  disabled?: boolean;
};

export const QaLanguageRow = ({
  languageConfig,
  globalSettings,
  disabled,
}: Props) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const project = useProject();
  const summaryText = useLanguageSettingsSummary(
    languageConfig,
    globalSettings
  );

  const toggleEnabledMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings/languages/{languageId}/enabled',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-settings',
  });

  function handleToggleEnabled() {
    toggleEnabledMutation.mutate({
      path: { projectId: project.id, languageId: languageConfig.language.id },
      content: { 'application/json': { enabled: !languageConfig.enabled } },
    });
  }

  return (
    <>
      <div className={TABLE_DIVIDER} />

      <div className={TABLE_FIRST_CELL}>
        <LanguageItem language={languageConfig.language} />
      </div>

      <Box sx={{ px: 2 }}>
        <Typography variant="body2" color="text.secondary">
          {summaryText}
        </Typography>
      </Box>

      <Box
        className={TABLE_LAST_CELL}
        sx={{ display: 'flex', alignItems: 'center' }}
      >
        <Switch
          checked={languageConfig.enabled}
          onChange={handleToggleEnabled}
          size="small"
          disabled={disabled || toggleEnabledMutation.isLoading}
          data-cy="qa-language-enabled-toggle"
          data-cy-language={languageConfig.language.tag}
        />
        <IconButton
          onClick={() => setDialogOpen(true)}
          size="small"
          disabled={disabled}
          data-cy="qa-language-settings-button"
          data-cy-language={languageConfig.language.tag}
        >
          <Settings01 />
        </IconButton>
      </Box>

      {dialogOpen && (
        <QaLanguageSettingsDialog
          onClose={() => setDialogOpen(false)}
          languageConfig={languageConfig}
          globalSettings={globalSettings}
        />
      )}
    </>
  );
};

function useLanguageSettingsSummary(
  languageConfig: LanguageQaConfigModel,
  globalSettings: Record<QaCheckType, QaCheckSeverity>
): string {
  const { t } = useTranslate();

  if (!languageConfig.enabled) {
    return t('project_settings_qa_languages_disabled');
  }

  if (!languageConfig.customSettings) {
    return t('project_settings_qa_languages_inherited');
  }
  const effectiveSettings: Record<QaCheckType, QaCheckSeverity> = {
    ...globalSettings,
    ...languageConfig.customSettings,
  };

  const values = Object.values(effectiveSettings);
  const warnings = values.filter((v) => v === 'WARNING').length;
  const off = values.filter((v) => v === 'OFF').length;

  if (warnings === 0) {
    return t('project_settings_qa_languages_all_off');
  }

  return t('project_settings_qa_languages_summary', {
    warnings,
    off,
  });
}
