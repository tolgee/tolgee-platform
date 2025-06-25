import { Box, styled, SxProps } from '@mui/material';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  padding: 6px 8px;
  align-items: start;
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

const StyledDate = styled('div')`
  font-size: 15px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  suggestion: TranslationSuggestionSimpleModel;
  isPlural: boolean;
  locale: string;
  maxLines?: number;
  lastUpdated?: number | string;
  sx?: SxProps;
};

export const TranslationSuggestion = ({
  suggestion,
  isPlural,
  locale,
  maxLines = 3,
  lastUpdated,
  sx,
}: Props) => {
  const formatDate = useTimeDistance();
  return (
    <StyledContainer {...{ sx }} data-cy="translation-suggestion">
      <AvatarImg owner={{ ...suggestion.author, type: 'USER' }} size={24} />
      <StyledContent>
        <TranslationVisual
          text={suggestion.translation}
          isPlural={isPlural}
          locale={locale}
          maxLines={maxLines}
          extraPadding={false}
        />
      </StyledContent>
      {lastUpdated && <StyledDate>{formatDate(lastUpdated)}</StyledDate>}
    </StyledContainer>
  );
};
