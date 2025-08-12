import { styled, Tooltip } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';
import { useTranslate } from '@tolgee/react';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
`;

const StyledExtra = styled('div')`
  display: flex;
  padding: 1px 8px;
  align-items: center;
  border-radius: 13px;
  background: ${({ theme }) => theme.palette.tokens.background.onDefaultGrey};
  margin-top: 8px;
  margin-left: 8px;
`;

const StyledWrapper = styled('div')`
  display: grid;
  gap: 8px;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.background.onDefaultGrey};
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
  const { t } = useTranslate();
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
      {Boolean(extraCount) && (
        <Tooltip title={t('suggestions_other_tooltip')}>
          <StyledExtra>+{extraCount}</StyledExtra>
        </Tooltip>
      )}
    </StyledContainer>
  );
};
