import {
  Box,
  Button,
  CircularProgress,
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useHistory } from 'react-router-dom';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useQaCheckTypes } from 'tg.globalContext/helpers';
import { getProjectTranslationsUrl, LINKS, PARAMS } from 'tg.constants/links';
import { QaCheck } from 'tg.component/CustomIcons';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { QaLanguageStatsProps } from '../../../eeSetup/EeModuleType';
import { IssueRow } from './IssueRow';
import { QaCheckType } from 'tg.service/apiSchemaTypes';

const StyledContent = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

const StyledHeader = styled(Box)`
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 0 8px;
`;

const StyledButtons = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
`;

const StyledSettingsLink = styled(Typography)`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  text-decoration: none;
  cursor: pointer;
` as typeof Typography;

export const QaLanguageStats = ({
  languageId,
  languageTag,
}: QaLanguageStatsProps) => {
  const { t } = useTranslate();
  const project = useProject();
  const history = useHistory();
  const allQaCheckTypes = useQaCheckTypes();
  const languages = useProjectLanguages();
  const baseLanguageTag = languages.find((l) => l.base)?.tag;
  const currentLanguage = languages.find((l) => l.id === languageId);
  const flagEmoji = currentLanguage?.flagEmoji;

  const { data, isLoading } = useApiQuery({
    url: '/v2/projects/{projectId}/stats/qa-issue-counts',
    method: 'get',
    path: { projectId: project.id },
    query: { languageId },
  });

  const entries = data
    ? (Object.entries(data) as [QaCheckType, number][])
        .filter(([, count]) => count > 0)
        .sort(([, a], [, b]) => b - a)
    : [];

  const targetLanguages = Array.from(
    new Set([baseLanguageTag, languageTag].filter(Boolean) as string[])
  );

  const navigateToTranslations = (filters: Record<string, unknown> = {}) => {
    history.push(
      getProjectTranslationsUrl(project.id, {
        languages: targetLanguages,
        filters,
      })
    );
  };

  return (
    <StyledContent>
      <StyledHeader>
        <QaCheck width={24} height={24} />
        <Typography variant="body2" fontWeight={500}>
          {t('qa_dashboard_popover_title')}
        </Typography>
        {flagEmoji && (
          <CircledLanguageIcon size={20} flag={flagEmoji} sx={{ ml: 'auto' }} />
        )}
      </StyledHeader>

      {isLoading ? (
        <Box display="flex" justifyContent="center" py={2}>
          <CircularProgress size={24} />
        </Box>
      ) : (
        <Box display="flex" flexDirection="column">
          {entries.map(([checkType, count]) => (
            <IssueRow
              key={checkType}
              checkType={checkType}
              count={count}
              onClick={() =>
                navigateToTranslations({ filterQaCheckTypes: [checkType] })
              }
            />
          ))}
          {entries.length === 0 && (
            <Box px={1}>
              <Typography variant="body2" color="text.secondary">
                {t('qa_dashboard_popover_no_issues')}
              </Typography>
            </Box>
          )}
        </Box>
      )}

      <StyledButtons>
        <StyledSettingsLink
          component={Link}
          to={LINKS.PROJECT_EDIT_QA.build({
            [PARAMS.PROJECT_ID]: project.id,
          })}
          variant="button"
        >
          {t('qa_dashboard_popover_settings')}
        </StyledSettingsLink>
        <Button
          variant="outlined"
          color="primary"
          size="small"
          onClick={() =>
            navigateToTranslations({ filterQaCheckTypes: allQaCheckTypes })
          }
        >
          {t('qa_dashboard_popover_show_all')}
        </Button>
      </StyledButtons>
    </StyledContent>
  );
};
