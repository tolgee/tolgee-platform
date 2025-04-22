import { Box, Card, styled, Typography } from '@mui/material';
import React from 'react';
import { GlossaryTooltipProps } from '../../../eeSetup/EeModuleType';
import { ArrowNarrowRight, BookClosed } from '@untitled-ui/icons-react';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { FlagImage } from 'tg.component/languages/FlagImage';

const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1.5)};
  padding: ${({ theme }) => theme.spacing(2)};
  border-radius: ${({ theme }) => theme.spacing(2)};
  margin-top: ${({ theme }) => theme.spacing(1)};
  max-width: 450px;
`;

const StyledInnerCard = styled(Card)`
  padding: ${({ theme }) => theme.spacing(1.5)};
  background-color: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledTitleWrapper = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  margin-right: ${({ theme }) => theme.spacing(2)};
`;

const StyledTitle = styled(Typography)``;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

export const GlossaryTooltip: React.VFC<GlossaryTooltipProps> = ({
  term,
  languageTag,
  targetLanguageTag,
}) => {
  const realLanguageTag = term.flagNonTranslatable
    ? term.glossary.baseLanguageTag
    : languageTag;
  const translation = term.translations.find(
    (t) => t.languageTag === realLanguageTag
  );
  const targetTranslation = term.translations.find(
    (t) => t.languageTag === targetLanguageTag
  );
  const targetLanguageFlag = targetLanguageTag
    ? languageInfo[targetLanguageTag]?.flags?.[0]
    : undefined;
  return (
    <StyledCard>
      <StyledTitleWrapper>
        <BookClosed />
        <StyledTitle variant="body2">{translation?.text}</StyledTitle>
        {targetTranslation &&
          languageTag != targetLanguageTag &&
          !term.flagNonTranslatable && (
            <>
              <ArrowNarrowRight />
              {targetLanguageFlag && (
                <FlagImage width={20} flagEmoji={targetLanguageFlag} />
              )}
              <StyledTitle variant="body2">
                {targetTranslation.text}
              </StyledTitle>
            </>
          )}
      </StyledTitleWrapper>
      <GlossaryTermTags term={term} />
      {term.description && (
        <StyledInnerCard>
          <StyledDescription>{term.description}</StyledDescription>
        </StyledInnerCard>
      )}
    </StyledCard>
  );
};
