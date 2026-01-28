import { FC } from 'react';
import { Box, styled, Typography } from '@mui/material';
import { TranslationList, StyledLanguageField } from './KeyPanelBase';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { BranchMergeKeyModel } from '../../types';
import { CellStateBar } from 'tg.views/projects/translations/cell/CellStateBar';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const TranslationRow = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1.5, 2, 1.5, 2.5)};
  position: relative;
`;

const LanguageLabel = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const EmptyTranslation = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.disabled};
  font-style: italic;
`;

const StyledTranslationList = styled(TranslationList)`
  & > div:last-child {
    border-bottom: 0;
  }
`;

type Props = {
  keyData: BranchMergeKeyModel;
  changedTranslations?: string[];
  showAll?: boolean;
  hideAllWhenFalse?: boolean;
};

export const KeyTranslations: FC<Props> = ({
  keyData,
  changedTranslations,
  showAll,
  hideAllWhenFalse,
}) => {
  const languages = useTranslationsSelector((c) => c.languages);
  const translationEntries = Object.entries(keyData.translations ?? {});
  const languageOrder = languages?.map((l) => l.tag) ?? [];
  const orderedTags =
    languageOrder.length > 0
      ? languageOrder.filter((tag) => keyData.translations?.[tag])
      : translationEntries.map(([tag]) => tag);

  const visibleTags =
    hideAllWhenFalse && !showAll
      ? []
      : showAll || !changedTranslations
      ? orderedTags
      : orderedTags.filter((tag) => changedTranslations.includes(tag));

  return (
    <StyledTranslationList>
      {visibleTags.map((lang) => {
        const language = languages?.find((l) => l.tag === lang);
        const translation = keyData.translations[lang];
        return (
          <StyledLanguageField
            key={lang}
            data-cy="translation-edit-translation-field"
          >
            <TranslationRow>
              <CellStateBar state={translation?.state} onResize={() => {}} />
              <Box>
                <LanguageLabel>
                  {language?.flagEmoji && (
                    <FlagImage flagEmoji={language.flagEmoji} height={16} />
                  )}
                  <span>{language?.name ?? lang}</span>
                </LanguageLabel>
                {translation?.text ? (
                  <TranslationVisual
                    text={translation.text}
                    locale={lang}
                    isPlural={keyData.keyIsPlural}
                  />
                ) : (
                  <EmptyTranslation variant="body2">
                    <br />
                  </EmptyTranslation>
                )}
              </Box>
            </TranslationRow>
          </StyledLanguageField>
        );
      })}
    </StyledTranslationList>
  );
};
