import { Add } from '@mui/icons-material';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { CELL_SHOW_ON_HOVER } from '../cell/styles';
import { Wrapper } from './Wrapper';

const StyledAddIcon = styled(Add)`
  font-size: 16px;
  padding: 2px;
  width: 20px;
  height: 20px;
`;

const StyledLabel = styled('div')`
  margin-top: -2px;
  margin-right: 6px;
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
      className={clsx(CELL_SHOW_ON_HOVER, className)}
    >
      <StyledAddIcon data-cy="translations-tags-add" />
      {withFullLabel && (
        <StyledLabel>
          <T>translations_tag_label</T>
        </StyledLabel>
      )}
    </Wrapper>
  );
};
