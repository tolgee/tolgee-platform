import { useState } from 'react';
import { Box, IconButton, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Settings01 } from '@untitled-ui/icons-react';

import { LanguageItem } from 'tg.component/languages/LanguageItem';
import {
  TABLE_DIVIDER,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
} from 'tg.component/languages/tableStyles';
import { QaLanguageSettingsDialog } from './QaLanguageSettingsDialog';
import { components } from 'tg.service/apiSchema.generated';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];
type LanguageQaConfigModel = components['schemas']['LanguageQaConfigModel'];

type Props = {
  languageConfig: LanguageQaConfigModel;
  globalSettings: Record<QaCheckType, QaCheckSeverity>;
};

export const QaLanguageRow = ({ languageConfig, globalSettings }: Props) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const summaryText = useLanguageSettingsSummary(
    languageConfig,
    globalSettings
  );

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

      <div className={TABLE_LAST_CELL}>
        <IconButton
          onClick={() => setDialogOpen(true)}
          size="small"
          data-cy="qa-language-settings-button"
          data-cy-language={languageConfig.language.tag}
        >
          <Settings01 />
        </IconButton>
      </div>

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

  if (!languageConfig.customSettings) {
    return t('project_settings_qa_languages_inherited');
  }
  const effectiveSettings: Record<QaCheckType, QaCheckSeverity> = {
    ...globalSettings,
    ...(languageConfig.customSettings ?? {}),
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
