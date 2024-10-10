import { DiffValue } from '../types';
import { styled } from '@mui/material';

const StyledContainer = styled('span')`
  word-break: break-word;
`;

const StyledNamespace = styled('span')`
  background: ${({ theme }) => theme.palette.emphasis[100]};
  max-height: 1.5em;
  padding: 0px 4px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  color: ${({ theme }) => theme.palette.activity.added};
`;

const StyledNamespaceRemoved = styled(StyledNamespace)`
  text-decoration: line-through;
  color: ${({ theme }) => theme.palette.activity.removed};
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

type Props = {
  input: DiffValue<any>;
  diffEnabled: boolean;
};

const NamespaceComponent: React.FC<Props> = ({ input }) => {
  const oldInput = input.old?.data?.name || input.old;
  const newInput = input.new?.data?.name || input.new;
  if (oldInput && newInput) {
    return (
      <StyledContainer>
        <StyledNamespaceRemoved>{oldInput}</StyledNamespaceRemoved>
        <StyledArrow>→</StyledArrow>
        <StyledNamespace>{newInput}</StyledNamespace>
      </StyledContainer>
    );
  } else if (oldInput) {
    return (
      <StyledContainer>
        <StyledNamespaceRemoved>{oldInput}</StyledNamespaceRemoved>
      </StyledContainer>
    );
  } else if (newInput) {
    return (
      <StyledContainer>
        <StyledNamespace>{newInput}</StyledNamespace>
      </StyledContainer>
    );
  } else {
    return null;
  }
};

export const getNamespaceChange = (
  input: DiffValue<any>,
  diffEnabled: boolean
) => {
  return <NamespaceComponent input={input} diffEnabled={diffEnabled} />;
};
