import { Plus } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { CELL_SHOW_ON_HOVER } from '../cell/styles';
import { Wrapper } from './Wrapper';

const StyledAddIcon = styled(Plus)`
  font-size: 16px;
  width: 16px;
  height: 16px;
`;

const StyledLabel = styled('div')`
  padding: 0 4px;
`;

type Props = {
  onClick: () => void;
  withFullLabel: boolean;
  className?: string;
};

export const TagAdd: React.FC<Props> = ({
  onClick,
  withFullLabel,
  className,
}) => {
  return (
    <Wrapper
      role="add"
      onClick={onClick}
      className={clsx(
        CELL_SHOW_ON_HOVER,
        className,
        withFullLabel && 'fullLabel'
      )}
    >
      <StyledAddIcon data-cy="translations-tags-add" />
      {withFullLabel && (
        <StyledLabel>
          <T keyName="translations_tag_label" />
        </StyledLabel>
      )}
    </Wrapper>
  );
};
