import { Box, styled, SxProps, Tooltip } from '@mui/material';
import { TranslationVisual } from './TranslationVisual';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  grid-template-columns: auto 1fr;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  align-items: start;
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

type Props = {
  suggestion: TranslationSuggestionSimpleModel;
  tooltip?: React.ReactNode;
  isPlural: boolean;
  locale: string;
  sx?: SxProps;
};

export const TranslationSuggestion = ({
  suggestion,
  tooltip,
  isPlural,
  locale,
  sx,
}: Props) => {
  const content = (
    <StyledContainer {...{ sx }} data-cy="ai-playground-preview">
      <AvatarImg owner={{ ...suggestion.author, type: 'USER' }} size={24} />
      <StyledContent>
        <TranslationVisual
          text={suggestion.translation}
          isPlural={isPlural}
          locale={locale}
          maxLines={3}
          extraPadding={false}
        />
      </StyledContent>
    </StyledContainer>
  );
  return tooltip ? <Tooltip title={tooltip}>{content}</Tooltip> : content;
};
