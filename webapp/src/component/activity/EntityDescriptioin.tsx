import { styled } from '@mui/material';
import { AnyReference } from './references/AnyReference';
import { Entity } from './types';

const StyledContainer = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  entity: Entity;
};

export const EntityDescription: React.FC<Props> = ({ entity }) => {
  const descriptionReferences = entity.references.filter((ref) =>
    entity.options.description?.includes(ref.type)
  );
  return (
    <StyledContainer>
      <AnyReference data={descriptionReferences} />
    </StyledContainer>
  );
};
