import { styled } from '@mui/material';
import { DiffValue } from '../types';

const StyledDiff = styled('span')`
  word-break: break-word;
  & .removed {
    text-decoration: line-through;
  }
  & .added {
  }
`;

export const getNoDiffChange = (input?: DiffValue) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (newInput) {
    return (
      <StyledDiff>
        <span className="added">{newInput}</span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <span className="removed">{oldInput}</span>
      </StyledDiff>
    );
  }
};
