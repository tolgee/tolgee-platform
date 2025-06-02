import { Plus } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';

const Wrap = styled('div')`
  display: flex;
  align-items: center;
  white-space: nowrap;
`;

const StyledLabel = styled('div')`
  margin-right: 6px;
`;

type Props = {
  onClick: () => void;
  className?: string;
};

export const AddLabel: React.FC<Props> = ({ onClick, className }) => {
  return (
    <Wrap onClick={onClick} className={className}>
      <Plus data-cy="translations-label-add" width={14} height={14} />
      <StyledLabel>
        <T keyName="translations_add_label" />
      </StyledLabel>
    </Wrap>
  );
};
