import { Plus } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';

const Wrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
`;

const StyledLabel = styled('div')`
  margin-right: 6px;
`;

type Props = {
  onClick: () => void;
  className?: string;
  showText?: boolean;
};

export const AddLabel: React.FC<Props> = ({ onClick, className, showText }) => {
  return (
    <Wrap
      onClick={onClick}
      className={className}
      data-cy="translation-label-add"
    >
      <Plus width={18} height={18} />
      {showText && (
        <StyledLabel>
          <T keyName="translations_add_label" />
        </StyledLabel>
      )}
    </Wrap>
  );
};
