import { styled, SxProps } from '@mui/material';

import { Wrapper } from './Wrapper';
import clsx from 'clsx';
import { CloseButton } from 'tg.component/common/buttons/CloseButton';
import { MouseEvent } from 'react';

const StyledTag = styled('div')`
  margin-left: 6px;
  margin-right: 6px;
  margin-top: -1px;
  flex-shrink: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledWrapper = styled(Wrapper)`
  &.selected {
    border-color: ${({ theme }) => theme.palette.primary.main};
    border-width: 1px;
  }
`;

type Props = {
  name: string;
  onDelete?: (e: MouseEvent) => void;
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
    <CloseButton onClose={onDelete} data-cy="translations-tag-close" xs>
      <StyledWrapper
        onClick={onClick ? () => onClick?.(name) : undefined}
        className={clsx({ selected }, className)}
        sx={sx}
      >
        <StyledTag>{name}</StyledTag>
      </StyledWrapper>
    </CloseButton>
  );
};
