import React, { FC } from 'react';
import {
  styled,
  Chip,
  Box,
  Tooltip,
  Button,
  Typography,
  IconButton,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useHistory } from 'react-router-dom';
import { Edit02 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguageMenu } from './LanguageMenu';
import { LanguageLabels } from './LanguageLabels';
import { TranslationStatesBar } from '../../TranslationStatesBar';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import clsx from 'clsx';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto auto auto 10fr auto;
  margin: ${({ theme }) => theme.spacing(1, 0, 2, 0)};
`;

const StyledRow = styled('div')`
  display: contents;
  & > * {
    padding-bottom: ${({ theme }) => theme.spacing(2)};
    padding-top: ${({ theme }) => theme.spacing(2)};
    cursor: pointer;
  }
  &.disabled > * {
    cursor: default;
    opacity: 0.6;
  }
  &:hover > * {
    background: ${({ theme }) => theme.palette.cell.hover};
  }
`;

const StyledInfo = styled(Box)`
  display: grid;
  grid-template-columns: auto auto 1fr;
  grid-template-areas:
    'name name name'
    'flag tag  base';
  gap: 5px 10px;
  padding-left: ${({ theme }) => theme.spacing(1)};
`;

const StyledTooltip = styled(Tooltip)`
  max-width: 100vw;
`;

const StyledStates = styled('div')`
  grid-column: 4;
  grid-row: span 2;
  padding: 0px 22px;
  padding-top: 10px;
  display: grid;
  align-items: center;
`;

const StyledActions = styled('div')`
  grid-column: 5;
  grid-row: span 2;
  display: grid;
  align-items: center;
  padding-right: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.divider};
`;

const StyledBottomButton = styled('div')`
  padding-top: ${({ theme }) => theme.spacing(2)};
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
`;

type Props = {
  languageStats: components['schemas']['LanguageStatsModel'][];
  wordCount: number;
};

export const LanguageStats: FC<Props> = ({ languageStats, wordCount }) => {
  const theme = useTheme();
  const languages = useProjectLanguages();
  const { satisfiesLanguageAccess, satisfiesPermission } =
    useProjectPermissions();
  const project = useProject();
  const { t } = useTranslate();
  const history = useHistory();
  const baseLanguage = languages.find((l) => l.base === true)!.tag;
  const allLangs = languages.map((l) => l.tag);
  const canViewLanguages = satisfiesPermission('translations.view');
  const canEditLanguages = satisfiesPermission('languages.edit');

  const redirectToLanguage = (lang?: string) => {
    const langs = !lang
      ? allLangs
      : lang === baseLanguage
      ? [lang]
      : [baseLanguage, lang];
    history.push(
      LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: project.id }) +
        '?' +
        langs.map((l) => `languages=${l}`).join('&')
    );
  };

  return (
    <StyledContainer>
      <Box display="flex" gridColumn="1 / -1" justifyContent="space-between">
        <Box display="flex" gap={1} alignItems="center">
          <Typography variant="h4">{t('dashboard_languages_title')}</Typography>
          <Chip
            data-cy="project-dashboard-language-count"
            size="small"
            label={languageStats.length}
            sx={{ background: theme.palette.tokens.text._states.selected }}
          />
        </Box>
        {canEditLanguages && (
          <IconButton
            data-cy="project-dashboard-languages-edit"
            component={Link}
            to={LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
          >
            <Edit02 width={20} height={20} />
          </IconButton>
        )}
      </Box>
      {languageStats.map((item, i) => {
        const language = languages.find((l) => l.id === item.languageId)!;
        const canViewLanguage = satisfiesLanguageAccess(
          'translations.view',
          language.id
        );

        return (
          <React.Fragment key={item.languageId}>
            <StyledRow
              className={clsx({ disabled: !canViewLanguage })}
              onClick={() =>
                canViewLanguage && redirectToLanguage(language.tag)
              }
            >
              <StyledInfo>
                <Box gridArea="name">
                  {item.languageName +
                    (item.languageOriginalName &&
                    item.languageOriginalName !== item.languageName
                      ? ' | ' + item.languageOriginalName
                      : '')}
                </Box>
                <Box gridArea="flag">
                  <CircledLanguageIcon
                    size={20}
                    flag={item.languageFlagEmoji || ''}
                  />
                </Box>
                <Box gridArea="tag">{item.languageTag}</Box>
                <Box gridArea="base">
                  {language?.base && (
                    <Chip size="small" label={t('global_language_base')} />
                  )}
                </Box>
              </StyledInfo>
              <StyledStates data-cy="project-dashboard-language-bar">
                <StyledTooltip
                  disableInteractive={true}
                  componentsProps={{
                    tooltip: { style: { maxWidth: '100vw' } },
                  }}
                  className="test"
                  title={<LanguageLabels data={item} />}
                >
                  <Box>
                    <TranslationStatesBar
                      labels={false}
                      hideTooltips={true}
                      stats={{
                        keyCount: wordCount,
                        languageCount: 1,
                        translationStatePercentages: {
                          TRANSLATED: item.translatedPercentage,
                          REVIEWED: item.reviewedPercentage,
                          UNTRANSLATED: item.untranslatedPercentage,
                        },
                      }}
                    />
                  </Box>
                </StyledTooltip>
              </StyledStates>
              <StyledActions>
                <LanguageMenu language={language!} />
              </StyledActions>
            </StyledRow>
            {i + 1 < languageStats.length && <StyledSeparator />}
          </React.Fragment>
        );
      })}
      {languageStats.length > 1 && canViewLanguages && (
        <>
          <StyledSeparator />
          <StyledBottomButton>
            <Button
              component={Link}
              to={LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              })}
            >
              {t('project_dashboard_show_translations')}
            </Button>
          </StyledBottomButton>
        </>
      )}
    </StyledContainer>
  );
};
