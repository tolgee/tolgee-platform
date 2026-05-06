import React, { useMemo } from 'react';
import { generatePlaceholdersStyle, getPlaceholders } from '@tginternal/editor';
import { styled, useTheme } from '@mui/material';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { placeholderToElement } from 'tg.views/projects/translations/translationVisual/placeholderToElement';

const StyledWrapper = styled('div')`
  white-space: pre-wrap;
`;

type Props = {
  text: string;
  locale: string;
};

/**
 * Renders TM entry text with ICU placeholders / variables highlighted.
 *
 * Slimmed-down counterpart to `TranslationWithPlaceholders` for the org-scoped TM views:
 * no `useProject()`, no glossary highlights, no QA issues, no plurals. ICU parsing is
 * always on — TM entries can be referenced from multiple projects and we have no signal
 * about each one's `icuPlaceholders` setting; rendering placeholders highlighted is safe
 * because raw text without placeholder syntax is unaffected.
 */
export const TmEntryText: React.VFC<Props> = ({ text, locale }) => {
  const theme = useTheme();
  const direction = getLanguageDirection(locale);

  const placeholders = useMemo(
    () => getPlaceholders(text, false) || [],
    [text]
  );

  const StyledPlaceholdersWrapper = useMemo(
    () =>
      generatePlaceholdersStyle({
        styled,
        colors: theme.palette.placeholders,
        component: StyledWrapper,
      }),
    [theme.palette.placeholders]
  );

  if (placeholders.length === 0) {
    return (
      <StyledPlaceholdersWrapper dir={direction} lang={locale}>
        {text}
      </StyledPlaceholdersWrapper>
    );
  }

  const sorted = [...placeholders].sort(
    (a, b) => a.position.start - b.position.start
  );
  const chunks: React.ReactNode[] = [];
  let index = 0;
  for (const placeholder of sorted) {
    if (placeholder.position.start < index) {
      continue;
    }
    if (placeholder.position.start > index) {
      chunks.push(text.substring(index, placeholder.position.start));
    }
    chunks.push(
      placeholderToElement({
        placeholder,
        key: placeholder.position.start,
      })
    );
    index = placeholder.position.end;
  }
  if (index < text.length) {
    chunks.push(text.substring(index));
  }

  return (
    <StyledPlaceholdersWrapper dir={direction} lang={locale}>
      {chunks}
    </StyledPlaceholdersWrapper>
  );
};
