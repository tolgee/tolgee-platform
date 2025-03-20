import { styled, SxProps } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

import { Wrapper } from './Wrapper';
import clsx from 'clsx';

const StyledTag = styled('div')`
  margin-left: 6px;
  margin-right: 6px;
  margin-top: -1px;
  flex-shrink: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledCloseIcon = styled(XClose)`
  margin-left: -5px;
  padding: 1px;
  cursor: pointer;
  width: 19px;
  height: 19px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledWrapper = styled(Wrapper)`
  &.selected {
    border-color: ${({ theme }) => theme.palette.primary.main};
    border-width: 1px;
  }
`;

type Props = {
  name: string;
  onDelete?: React.MouseEventHandler<SVGElement>;
  onClick?: (name: string) => void;
  selected?: boolean;
  className?: string;
  sx?: SxProps;
};

export const Tag: React.FC<Props> = ({
  name,
  onDelete,
  onClick,
  selected,
  className,
  sx,
}) => {
  return (
    <StyledWrapper
      onClick={onClick ? () => onClick?.(name) : undefined}
      className={clsx({ selected }, className)}
      sx={sx}
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
