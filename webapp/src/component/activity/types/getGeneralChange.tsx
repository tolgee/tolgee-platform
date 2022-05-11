import { styled } from '@mui/material';
import { DiffValue } from '../types';

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

export const getGeneralChange = (input?: DiffValue) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (oldInput && newInput) {
    return (
      <StyledDiff>
        <StyledRemoved>{oldInput}</StyledRemoved>
        <StyledArrow>→</StyledArrow>
        <span>{newInput}</span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <StyledRemoved>{oldInput}</StyledRemoved>
      </StyledDiff>
    );
  } else if (newInput) {
    return (
      <StyledDiff>
        <span>{newInput}</span>
      </StyledDiff>
    );
  }
};
