import React, { FC } from 'react';
import { styled, Chip, Box, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguageMenu } from './LanguageMenu';
import { LanguageLabels } from './LanguageLabels';
import { TranslationStatesBar } from '../../TranslationStatesBar';

const StyledContainer = styled('div')`
  display: grid;
  gap: 5px 10px;
  grid-template-columns: auto auto auto 10fr auto;
  margin: ${({ theme }) => theme.spacing(2, 0)};
  align-items: start;
`;

const StyledInfo = styled(Box)`
  display: grid;
  grid-template-columns: auto auto 1fr;
  grid-template-areas:
    'name name name'
    'flag tag  base';
  gap: 5px 10px;
`;

const StyledTooltip = styled(Tooltip)`
  max-width: 100vw;
`;

const StyledStates = styled('div')`
  grid-column: 4;
  grid-row: span 2;
  margin: 0px 22px;
  margin-top: 10px;
  align-self: center;
`;

const StyledActions = styled('div')`
  grid-column: 5;
  grid-row: span 2;
  margin-top: 5px;
`;

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.divider};
  margin: ${({ theme }) => theme.spacing(2, 0)};
`;

type Props = {
  languageStats: components['schemas']['LanguageStatsModel'][];
  wordCount: number;
};

export const LanguageStats: FC<Props> = ({ languageStats, wordCount }) => {
  const languages = useProjectLanguages();
  const t = useTranslate();

  return (
    <StyledContainer>
      {languageStats.map((item, i) => {
        const language = languages.find((l) => l.id === item.languageId);

        return (
          <React.Fragment key={item.languageId}>
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
            <StyledTooltip
              componentsProps={{ tooltip: { style: { maxWidth: '100vw' } } }}
              className="test"
              title={<LanguageLabels data={item} />}
            >
              <StyledStates data-cy="project-dashboard-language-bar">
                <TranslationStatesBar
                  labels={false}
                  hideTooltips={true}
                  stats={{
                    keyCount: wordCount,
                    languageCount: 1,
                    translationStateCounts: {
                      TRANSLATED: item.translatedWordCount,
                      REVIEWED: item.reviewedWordCount,
                      UNTRANSLATED: item.untranslatedWordCount,
                    },
                  }}
                />
              </StyledStates>
            </StyledTooltip>
            <StyledActions>
              <LanguageMenu language={language!} />
            </StyledActions>

            {i + 1 < languageStats.length && <StyledSeparator />}
          </React.Fragment>
        );
      })}
    </StyledContainer>
  );
};
