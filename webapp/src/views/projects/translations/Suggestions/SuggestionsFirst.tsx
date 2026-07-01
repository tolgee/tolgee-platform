import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';
import { useTranslate } from '@tolgee/react';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

// Mirror of TranslationSuggestionServiceEeImpl.MAX_DISPLAYED_SUGGESTIONS (backend caps the embed); keep in sync.
export const MAX_DISPLAYED_SUGGESTIONS = 3;

const StyledWrapper = styled('div')`
  display: grid;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.background.onDefaultGrey};
`;

const StyledShowAll = styled('button')`
  border: none;
  background: none;
  cursor: pointer;
  font-family: inherit;
  padding: 6px 0;
  font-size: 13px;
  font-weight: ${({ theme }) => theme.typography.button.fontWeight};
  letter-spacing: 0.46px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};

  &:hover {
    color: ${({ theme }) => theme.palette.text.primary};
  }
`;

type Props = {
  count: number;
  suggestions: TranslationSuggestionSimpleModel[];
  isPlural: boolean;
  locale: string;
  onShowAll?: () => void;
};

export const SuggestionsFirst = ({
  count,
  suggestions,
  isPlural,
  locale,
  onShowAll,
}: Props) => {
  const { t } = useTranslate();
  const displayed = suggestions.slice(0, MAX_DISPLAYED_SUGGESTIONS);
  const hasMore = count > displayed.length;
  return (
    <StyledWrapper>
      {displayed.map((s) => (
        <TranslationSuggestion
          key={s.id}
          suggestion={s}
          isPlural={isPlural}
          locale={locale}
          maxLines={2}
        />
      ))}
      {hasMore && onShowAll && (
        <StyledShowAll
          type="button"
          data-cy="suggestions-show-all"
          onClick={(e) => {
            e.stopPropagation();
            onShowAll();
          }}
        >
          {t('translation_tools_suggestions_show_all_label')}
        </StyledShowAll>
      )}
    </StyledWrapper>
  );
};
