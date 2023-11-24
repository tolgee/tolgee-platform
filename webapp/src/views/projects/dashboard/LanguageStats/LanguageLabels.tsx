import React from 'react';
import { T } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { StateType, TRANSLATION_STATES } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { PercentFormat } from '../PercentFormat';

const DOT_SIZE = 8;

type LanguageStatsModel = components['schemas']['LanguageStatsModel'];

const StyledContainer = styled('div')`
  margin: 8px;
  display: grid;
  grid-template-columns: auto auto 1fr 1fr 1fr;
  gap: 5px;
  align-items: center;
  white-space: nowrap;
`;

const StyledDot = styled(Box)`
  width: ${DOT_SIZE}px;
  height: ${DOT_SIZE}px;
  border-radius: ${DOT_SIZE / 2}px;
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.8 : 1)}
  );
`;

type StatItem = {
  status: StateType;
  percent: number;
  keyCount: number;
  wordCount: number | null;
};

const statsIntoArray = (data: LanguageStatsModel) => {
  const statItems: StatItem[] = [];
  statItems.push({
    status: 'REVIEWED',
    percent: data.reviewedPercentage,
    keyCount: data.reviewedKeyCount,
    wordCount: data.reviewedWordCount,
  });

  statItems.push({
    status: 'TRANSLATED',
    percent: data.translatedPercentage,
    keyCount: data.translatedKeyCount,
    wordCount: data.translatedWordCount,
  });

  statItems.push({
    status: 'UNTRANSLATED',
    percent: data.untranslatedPercentage,
    keyCount: data.untranslatedKeyCount,
    wordCount: data.untranslatedWordCount,
  });

  return statItems.filter((i) => i.keyCount > 0);
};

type Props = {
  data: LanguageStatsModel;
};

export const LanguageLabels: React.FC<Props> = ({ data }) => {
  const statItems = statsIntoArray(data);

  return (
    <>
      {statItems.length ? (
        <StyledContainer>
          {statItems.map((item) => (
            <React.Fragment key={item.status}>
              <StyledDot
                gridColumn={1}
                sx={{ background: TRANSLATION_STATES[item.status].color }}
              />
              <Box data-cy="project-dashboard-language-label-state">
                {TRANSLATION_STATES[item.status].translation}
              </Box>
              <Box ml={2} data-cy="project-dashboard-language-label-percentage">
                <PercentFormat number={item.percent} />
              </Box>
              <Box ml={2} data-cy="project-dashboard-language-label-keys">
                <T
                  keyName="project_dashboard_language_keys"
                  params={{ count: item.keyCount }}
                />
              </Box>
              {item.wordCount !== null && (
                <Box ml={2} data-cy="project-dashboard-language-label-words">
                  <T
                    keyName="project_dashboard_language_words"
                    params={{ count: item.wordCount }}
                  />
                </Box>
              )}
            </React.Fragment>
          ))}
        </StyledContainer>
      ) : (
        <T keyName="project_dashboard_language_keys" params={{ count: 0 }} />
      )}
    </>
  );
};
