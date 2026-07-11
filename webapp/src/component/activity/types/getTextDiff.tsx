import { styled } from '@mui/material';
import { diffWordsWithSpace } from 'diff';

import { DiffValue } from '../types';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';

const maxDiffLength = 1_000;

const StyledRemoved = styled('span')`
  color: ${({ theme }) => theme.palette.activity.removed};
  text-decoration: line-through;
`;

const StyledAdded = styled('span')`
  background: ${({ theme }) => theme.palette.activity.addedHighlight};
`;

export const getTextDiff = (
  input?: DiffValue,
  languageTag?: string | undefined
) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  const dir = languageTag ? getLanguageDirection(languageTag) : undefined;
  if (oldInput && newInput) {
    const diffed = safeDiffWordsWithSpace(oldInput, newInput);
    return (
      <span dir={dir}>
        {diffed.map((part, i) =>
          part.added ? (
            <StyledAdded key={i}>{part.value}</StyledAdded>
          ) : part.removed ? (
            <StyledRemoved key={i}>{part.value}</StyledRemoved>
          ) : (
            <span key={i}>{part.value}</span>
          )
        )}
      </span>
    );
  } else if (oldInput) {
    return (
      <span dir={dir}>
        <StyledRemoved>{oldInput}</StyledRemoved>
      </span>
    );
  } else if (newInput) {
    return (
      <span dir={dir}>
        <StyledAdded>{newInput}</StyledAdded>
      </span>
    );
  }
};

/**
 * Diffing is very expensive for large texts - freezes the UI.
 */
function safeDiffWordsWithSpace(oldInput: string, newInput: string) {
  const tooLong = newInput.length + oldInput.length > maxDiffLength;
  if (tooLong) {
    return [
      { value: newInput, added: true },
      { value: oldInput, removed: true },
    ];
  }

  return diffWordsWithSpace(oldInput, newInput);
}
