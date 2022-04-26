import React, { FC } from 'react';
import { styled, Chip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguageMenu } from './LanguageMenu';
import { TranslationStatesBar } from '../../TranslationStatesBar';

const StyledContainer = styled('div')`
  display: grid;
  gap: 5px 10px;
  grid-template-columns: auto auto auto 10fr auto;
`;

const StyledLangName = styled('div')`
  grid-column: 1 / span 3;
`;

const StyledLangShortcut = styled('div')`
  grid-column: 1;
`;

const StyledLangFlag = styled('div')`
  grid-column: 2;
`;

const StyledBase = styled('div')`
  grid-column: 3;
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
  keyCount: number;
};

export const LanguageStats: FC<Props> = ({ languageStats, keyCount }) => {
  const languages = useProjectLanguages();
  const t = useTranslate();

  return (
    <StyledContainer>
      {languageStats.map((languageStats) => {
        const untranslatedCount =
          keyCount -
          (languageStats.translatedKeyCount + languageStats.reviewedKeyCount);

        const language = languages.find(
          (l) => l.id === languageStats.languageId
        );

        return (
          <React.Fragment key={languageStats.languageId}>
            <StyledLangName>
              {languageStats.languageName +
                (languageStats.languageOriginalName &&
                languageStats.languageOriginalName !==
                  languageStats.languageName
                  ? ' | ' + languageStats.languageOriginalName
                  : '')}
            </StyledLangName>
            <StyledStates>
              <TranslationStatesBar
                labels={true}
                stats={{
                  keyCount: keyCount,
                  languageCount: 1,
                  translationStateCounts: {
                    TRANSLATED: languageStats.translatedKeyCount,
                    REVIEWED: languageStats.reviewedKeyCount,
                    UNTRANSLATED: untranslatedCount,
                  },
                }}
              />
            </StyledStates>
            <StyledActions>
              <LanguageMenu language={language!} />
            </StyledActions>
            <StyledLangShortcut>{languageStats.languageTag}</StyledLangShortcut>
            <StyledLangFlag>
              <CircledLanguageIcon
                size={20}
                flag={languageStats.languageFlagEmoji || ''}
              />
            </StyledLangFlag>
            <StyledBase>
              {language?.base && (
                <Chip size="small" label={t('global_language_base')} />
              )}
            </StyledBase>
            <StyledSeparator />
          </React.Fragment>
        );
      })}
    </StyledContainer>
  );
};
