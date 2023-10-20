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
`;

type Props = {
  input: DiffValue<any>;
};

const NamespaceComponent: React.FC<Props> = ({ input }) => {
  const newInput = input.new?.data?.name || input.new;
  if (newInput) {
    return (
      <StyledContainer>
        <StyledNamespace>{newInput}</StyledNamespace>
      </StyledContainer>
    );
  } else {
    return null;
  }
};

export const getBatchNamespaceChange = (input: DiffValue<any>) => {
  return <NamespaceComponent input={input} />;
};
