import { styled } from '@mui/material';

import { DiffValue } from '../types';

const StyledTag = styled('span')`
  display: inline-flex;
  border-radius: 12px;
  padding: 4px 8px;
  align-items: center;
  height: 24px;
  font-size: 14px;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  position: relative;

  & + & {
    margin-left: 3px;
  }
`;

export const getBatchKeyTagListChange = (input?: DiffValue<string[]>) => {
  const newInput = input?.new || [];

  return (
    <>
      {newInput.map((tag, i) => (
        <StyledTag key={i}>{tag}</StyledTag>
      ))}
    </>
  );
};
