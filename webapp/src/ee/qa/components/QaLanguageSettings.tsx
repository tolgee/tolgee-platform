import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  StyledLanguageTable,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
} from 'tg.component/languages/tableStyles';
import { QaLanguageRow } from './QaLanguageRow';
import { components } from 'tg.service/apiSchema.generated';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

type Props = {
  globalSettings: Record<QaCheckType, QaCheckSeverity>;
};

export const QaLanguageSettings = ({ globalSettings }: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const languageSettingsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/qa-settings/languages',
    method: 'get',
    path: { projectId: project.id },
  });

  if (!languageSettingsLoadable.data) {
    return null;
  }

  const languageSettings = languageSettingsLoadable.data;

  const summary = getGlobalSummary(globalSettings);

  return (
    <Box sx={{ mt: 4 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        <T keyName="project_settings_qa_languages_title" />
      </Typography>

      <StyledLanguageTable style={{ gridTemplateColumns: '1fr 1fr auto' }}>
        {/* Global QA header row */}
        <div className={TABLE_FIRST_CELL}>
          <Typography variant="body1" fontWeight="bold">
            <T keyName="project_settings_qa_languages_global_qa" />
          </Typography>
        </div>
        <Box sx={{ px: 2 }}>
          <Typography variant="body2" color="text.secondary">
            {t('project_settings_qa_languages_summary', summary)}
          </Typography>
        </Box>
        <div className={TABLE_LAST_CELL} />

        {languageSettings.map((langSetting) => (
          <QaLanguageRow
            key={langSetting.language.id}
            languageConfig={langSetting}
            globalSettings={globalSettings}
          />
        ))}
      </StyledLanguageTable>
    </Box>
  );
};

function getGlobalSummary(
  globalSettings: Record<QaCheckType, QaCheckSeverity>
): { warnings: number; off: number } {
  const values = Object.values(globalSettings);
  const warnings = values.filter((v) => v === 'WARNING').length;
  const off = values.filter((v) => v === 'OFF').length;
  return { warnings, off };
}
