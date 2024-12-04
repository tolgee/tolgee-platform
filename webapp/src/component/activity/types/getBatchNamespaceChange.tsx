import { StyledReferences } from '../references/AnyReference';
import { DiffValue } from '../types';

type Props = {
  input: DiffValue<any>;
};

const NamespaceComponent: React.FC<Props> = ({ input }) => {
  const newInput = input.new?.data?.name || input.new;
  if (newInput) {
    return (
      <StyledReferences>
        <span className="reference">{newInput}</span>
      </StyledReferences>
    );
  } else {
    return null;
  }
};

export const getBatchNamespaceChange = (input: DiffValue<any>) => {
  return <NamespaceComponent input={input} />;
};
