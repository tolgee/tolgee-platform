import {
  Box,
  Card,
  IconButton,
  styled,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import React from 'react';
import { GlossaryTermPreviewProps } from '../../../eeSetup/EeModuleType';
import {
  ArrowNarrowRight,
  BookClosed,
  LinkExternal02,
} from '@untitled-ui/icons-react';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { getGlossaryTermSearchUrl } from 'tg.constants/links';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1.5)};
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
  showIcon,
}) => {
  const theme = useTheme();
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
    <StyledContainer>
      <StyledTitleWrapper>
        {showIcon && <BookClosed />}
        <StyledTitleTextWrapper>
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
        </StyledTitleTextWrapper>
        <StyledGap />
        <Tooltip
          title={<T keyName="glossary_term_preview_open_full_view_tooltip" />}
        >
          <IconButton
            sx={{
              marginRight: theme.spacing(-1),
            }}
            component={Link}
            to={getGlossaryTermSearchUrl(
              term.glossary.organizationOwner.slug,
              term.glossary.id,
              translation?.text || ''
            )}
          >
            <LinkExternal02 />
          </IconButton>
        </Tooltip>
      </StyledTitleWrapper>
      <GlossaryTermTags term={term} />
      <StyledInnerCard elevation={0}>
        {term.description ? (
          <StyledDescription>{term.description}</StyledDescription>
        ) : (
          <StyledEmptyDescription>
            <T keyName="empty_description" />
          </StyledEmptyDescription>
        )}
      </StyledInnerCard>
    </StyledContainer>
  );
};
