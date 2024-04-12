import { styled } from '@mui/material';
import { DiffValue } from '../types';
import { T } from '@tolgee/react';

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

type NamespaceType = {
  data: { name: string };
};

export const getDefaultNamespaceChange = (input?: DiffValue<NamespaceType>) => {
  const oldInput = input?.old?.data.name ?? <T keyName="namespace_default" />;
  const newInput = input?.new?.data.name ?? <T keyName="namespace_default" />;
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
