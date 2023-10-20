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

const StyledTagRemoved = styled(StyledTag)`
  text-decoration: line-through;
`;

const StyledAdded = styled('span')`
  color: ${({ theme }) => theme.palette.activity.added};
  margin-right: 5px;
`;

export const getKeyTagsChange = (input?: DiffValue<string[]>) => {
  const oldInput = input?.old || [];
  const newInput = input?.new || [];

  const removed: string[] = [];
  const added: string[] = [];

  newInput.forEach((tag) => {
    if (!oldInput.includes(tag)) {
      added.push(tag);
    }
  });

  oldInput.forEach((tag) => {
    if (!newInput.includes(tag)) {
      removed.push(tag);
    }
  });

  return (
    <>
      {removed.map((tag, i) => (
        <StyledTagRemoved key={i}>{tag}</StyledTagRemoved>
      ))}
      {added.map((tag, i) => (
        <StyledTag key={i}>
          <StyledAdded>+</StyledAdded>
          {tag}
        </StyledTag>
      ))}
    </>
  );
};
