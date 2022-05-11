import { styled } from '@mui/material';
import { DiffValue } from '../types';

const StyledDiff = styled('span')`
  word-break: break-word;
  & .removed {
    text-decoration: line-through;
  }
  & .added {
  }
  & .arrow {
    padding: 0px 6px;
  }
`;

export const getGeneralChange = (input?: DiffValue) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (oldInput && newInput) {
    return (
      <StyledDiff>
        <span className="removed">{oldInput}</span>
        <span className="arrow">→</span>
        <span className="added">{newInput}</span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <span className="removed">{oldInput}</span>
      </StyledDiff>
    );
  } else if (newInput) {
    return (
      <StyledDiff>
        <span className="added">{newInput}</span>
      </StyledDiff>
    );
  }
};
