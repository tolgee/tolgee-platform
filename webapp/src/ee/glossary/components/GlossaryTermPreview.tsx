import {
  Box,
  Card,
  IconButton,
  styled,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import React, { useState } from 'react';
import { GlossaryTermPreviewProps } from '../../../eeSetup/EeModuleType';
import {
  ArrowNarrowRight,
  BookClosed,
  LinkExternal02,
  InfoCircle,
} from '@untitled-ui/icons-react';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { getGlossaryTermSearchUrl } from 'tg.constants/links';
import clsx from 'clsx';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1.5)};

  border-radius: 4px;
  transition: background-color 0.1s ease-in-out, color 0.1s ease-in-out;

  &.slim {
    padding: ${({ theme }) => theme.spacing(0.75)};
    gap: ${({ theme }) => theme.spacing(0.5)};

    &:hover {
      background-color: ${({ theme }) => theme.palette.emphasis[50]};
    }
  }

  &.slim.clickable {
    cursor: pointer;

    &:hover {
      color: ${({ theme }) => theme.palette.primary.main};
    }
  }
`;

const StyledInnerCard = styled(Card)`
  padding: ${({ theme }) => theme.spacing(1.5)};
  background-color: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledTitleWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledTitleTextWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  margin-right: ${({ theme }) => theme.spacing(2)};
`;

const StyledTitle = styled(Typography)``;

const StyledGap = styled('div')`
  flex-grow: 1;
`;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledEmptyDescription = styled(StyledDescription)`
  font-style: italic;
`;

export const GlossaryTermPreview: React.VFC<GlossaryTermPreviewProps> = ({
  term,
  languageTag,
  targetLanguageTag,
  appendValue,
  standalone,
  slim,
}) => {
  const theme = useTheme();
  const [isHovering, setIsHovering] = useState(false);
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
  const text = targetTranslation?.text || translation?.text || undefined;
  const clickable = appendValue && text && text.length > 0;
  return (
    <StyledContainer
      data-cy="glossary-term-preview-container"
      className={clsx({ slim, clickable })}
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
      onMouseDown={(e) => {
        if (clickable) {
          e.preventDefault();
        }
      }}
      onClick={() => {
        if (clickable) {
          appendValue(text);
        }
      }}
    >
      <StyledTitleWrapper>
        {standalone && <BookClosed />}
        <StyledTitleTextWrapper>
          <StyledTitle
            variant="body2"
            data-cy="glossary-term-preview-source-text"
          >
            {translation?.text}
          </StyledTitle>
          {targetTranslation &&
            languageTag != targetLanguageTag &&
            !term.flagNonTranslatable && (
              <>
                <ArrowNarrowRight />
                {targetLanguageFlag && (
                  <FlagImage width={20} flagEmoji={targetLanguageFlag} />
                )}
                <StyledTitle
                  variant="body2"
                  data-cy="glossary-term-preview-target-text"
                >
                  {targetTranslation.text}
                </StyledTitle>
              </>
            )}
        </StyledTitleTextWrapper>
        <StyledGap />
        {(isHovering || standalone) && (
          <>
            {slim && term.description && (
              <Tooltip title={term.description}>
                <IconButton
                  sx={{
                    margin: theme.spacing(-0.8),
                  }}
                  size="small"
                >
                  <InfoCircle width={20} height={20} />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip
              title={
                <T keyName="glossary_term_preview_open_full_view_tooltip" />
              }
            >
              <IconButton
                sx={{
                  margin: theme.spacing(-0.8),
                }}
                component={Link}
                to={getGlossaryTermSearchUrl(
                  term.glossary.organizationOwner.slug,
                  term.glossary.id,
                  translation?.text || ''
                )}
                size="small"
              >
                <LinkExternal02 width={20} height={20} />
              </IconButton>
            </Tooltip>
          </>
        )}
      </StyledTitleWrapper>
      <GlossaryTermTags term={term} />
      {!slim && (
        <StyledInnerCard
          elevation={0}
          data-cy="glossary-term-preview-description-card"
        >
          {term.description ? (
            <StyledDescription data-cy="glossary-term-preview-description">
              {term.description}
            </StyledDescription>
          ) : (
            <StyledEmptyDescription data-cy="glossary-term-preview-empty-description">
              <T keyName="empty_description" />
            </StyledEmptyDescription>
          )}
        </StyledInnerCard>
      )}
    </StyledContainer>
  );
};
