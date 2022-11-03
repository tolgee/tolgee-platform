import { styled } from '@mui/material';
import { DiffValue } from '../types';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

export const getNoDiffChange = (input?: DiffValue, languageTag?: string) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  const dir = languageTag ? getLanguageDirection(languageTag) : undefined;
  if (newInput) {
    return (
      <StyledDiff>
        <span className="added" dir={dir}>
          {newInput}
        </span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <StyledRemoved dir={dir}>{oldInput}</StyledRemoved>
      </StyledDiff>
    );
  }
};
