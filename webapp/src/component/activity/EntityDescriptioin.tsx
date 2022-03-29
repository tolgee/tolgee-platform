import { styled } from '@mui/material';
import { AnyReference } from './references/AnyReference';
import { Entity } from './types';

const StyledContainer = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  & .reference {
    background: ${({ theme }) => theme.palette.emphasis[200]};
    padding: 0px 4px;
    border-radius: 4px;
    border: 1px solid ${({ theme }) => theme.palette.emphasis[300]};
  }

  & :not(.referenceLink) {
    color: ${({ theme }) => theme.palette.text.primary};
  }
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
