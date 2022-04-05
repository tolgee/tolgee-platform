import { styled } from '@mui/material';
import { Close } from '@mui/icons-material';

import { Wrapper } from './Wrapper';
import clsx from 'clsx';

type Props = {
  name: string;
  onDelete?: React.MouseEventHandler<SVGElement>;
  onClick?: (name: string) => void;
  selected?: boolean;
};

const StyledTag = styled('div')`
  margin-left: 6px;
  margin-right: 6px;
  margin-top: -1px;
  flex-shrink: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledCloseIcon = styled(Close)`
  margin-left: -6px;
  padding: 1px;
  cursor: pointer;
  width: 20px;
  height: 20px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledWrapper = styled(Wrapper)`
  &.selected {
    border-color: ${({ theme }) => theme.palette.primary.main};
    border-width: 1px;
  }
`;

export const Tag: React.FC<Props> = ({ name, onDelete, onClick, selected }) => {
  return (
    <StyledWrapper
      onClick={onClick ? () => onClick?.(name) : undefined}
      className={clsx({ selected })}
    >
      <StyledTag>{name}</StyledTag>
      {onDelete && (
        <StyledCloseIcon
          role="button"
          data-cy="translations-tag-close"
          onClick={onDelete}
        />
      )}
    </StyledWrapper>
  );
};
