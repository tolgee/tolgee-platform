import { Box, IconButton, styled, SxProps } from '@mui/material';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { Check, X } from '@untitled-ui/icons-react';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  padding: 6px 8px;
  align-items: start;

  & .actions,
  & .date {
    transition: opacity 0.1s ease-in-out;
  }
  & .actions {
    opacity: 0;
  }

  &:hover,
  &:focus-within {
    .actions {
      opacity: 1;
    }
    .date {
      opacity: 0;
    }
  }
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

const StyledRightPart = styled('div')`
  display: grid;
  grid-template-areas: rightPart;
  justify-items: end;
`;

const StyledDate = styled('div')`
  padding-top: 3px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
  grid-area: rightPart;
`;

const StyledActions = styled('div')`
  padding: 1px 0px;
  display: flex;
  grid-area: rightPart;
  gap: 4px;
`;

type Props = {
  suggestion: TranslationSuggestionSimpleModel;
  isPlural: boolean;
  locale: string;
  maxLines?: number;
  lastUpdated?: number | string;
  onDecline?: () => void;
  onAccept?: () => void;
  sx?: SxProps;
  className?: string;
};

export const TranslationSuggestion = ({
  suggestion,
  isPlural,
  locale,
  maxLines = 3,
  onAccept,
  onDecline,
  lastUpdated,
  sx,
  className,
}: Props) => {
  const formatDate = useTimeDistance();
  return (
    <StyledContainer {...{ sx, className }} data-cy="translation-suggestion">
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
      {lastUpdated && (
        <StyledRightPart>
          <StyledDate className="date">{formatDate(lastUpdated)}</StyledDate>
          <StyledActions className="actions">
            <IconButton
              onClick={onAccept}
              size="small"
              sx={{ margin: '-4px' }}
              color="success"
            >
              <Check width={20} height={20} />
            </IconButton>
            <IconButton
              onClick={onDecline}
              size="small"
              sx={{ margin: '-4px' }}
            >
              <X width={20} height={20} />
            </IconButton>
          </StyledActions>
        </StyledRightPart>
      )}
    </StyledContainer>
  );
};
