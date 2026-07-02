import { styled, Tooltip } from '@mui/material';
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

const StyledLastLine = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;

  & > :first-child {
    flex: 1;
    min-width: 0;
  }
`;

const StyledExtra = styled('div')`
  display: flex;
  flex-shrink: 0;
  height: 22px;
  min-width: 22px;
  padding: 0 4px;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  margin-right: 8px;
  font-size: 12px;
  line-height: 1;
  color: ${({ theme }) => theme.palette.text.secondary};
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
  border: 1px solid ${({ theme }) => theme.palette.divider};
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
  const displayed = suggestions.slice(0, MAX_DISPLAYED_SUGGESTIONS);
  const extraCount = count - displayed.length;
  return (
    <StyledWrapper>
      {displayed.map((s, i) => {
        const node = (
          <TranslationSuggestion
            key={s.id}
            suggestion={s}
            isPlural={isPlural}
            locale={locale}
            maxLines={2}
          />
        );
        const isLastLineWithExtra =
          extraCount > 0 && i === displayed.length - 1;
        if (isLastLineWithExtra) {
          return (
            <StyledLastLine key={s.id}>
              {node}
              <Tooltip title={t('suggestions_other_tooltip')}>
                <StyledExtra>+{extraCount}</StyledExtra>
              </Tooltip>
            </StyledLastLine>
          );
        }
        return node;
      })}
    </StyledWrapper>
  );
};
