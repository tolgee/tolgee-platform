import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: start;
`;

const StyledExtra = styled('div')`
  display: flex;
  padding: 1px 8px;
  align-items: center;
  border-radius: 13px;
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  margin-top: 10px;
`;

const StyledWrapper = styled('div')`
  display: grid;
  padding: 6px 8px;
  gap: 8px;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

type Props = {
  count: number;
  suggestions: TranslationSuggestionSimpleModel[];
  isPlural: boolean;
  locale: string;
};

export const SuggestionsFirst = ({
  count,
  suggestions,
  isPlural,
  locale,
}: Props) => {
  const extraCount = count - suggestions.length;
  return (
    <StyledContainer>
      <StyledWrapper>
        {suggestions.map((s) => (
          <TranslationSuggestion
            key={s.id}
            suggestion={s}
            isPlural={isPlural}
            locale={locale}
            maxLines={2}
          />
        ))}
      </StyledWrapper>
      {Boolean(extraCount) && <StyledExtra>+{extraCount}</StyledExtra>}
    </StyledContainer>
  );
};
