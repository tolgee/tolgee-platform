import { styled } from '@mui/material';
import { diffWordsWithSpace } from 'diff';

import { DiffValue } from '../types';

const StyledRemoved = styled('span')`
  color: ${({ theme }) => theme.palette.activity.removed};
  text-decoration: line-through;
`;

const StyledAdded = styled('span')`
  background: ${({ theme }) => theme.palette.activity.addedHighlight};
`;

export const getTextDiff = (input?: DiffValue) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (oldInput && newInput) {
    const diffed = diffWordsWithSpace(oldInput, newInput);
    return (
      <span>
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
      <span>
        <StyledRemoved>{oldInput}</StyledRemoved>
      </span>
    );
  } else if (newInput) {
    return (
      <span>
        <StyledAdded>{newInput}</StyledAdded>
      </span>
    );
  }
};
