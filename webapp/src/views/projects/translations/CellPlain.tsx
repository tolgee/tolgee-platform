import { Box } from '@material-ui/core';
import React from 'react';

type Props = {
  onClick?: React.MouseEventHandler<HTMLElement>;
  background?: string;
};

export const CellPlain: React.FC<Props> = ({
  children,
  onClick,
  background,
}) => {
  return (
    <Box
      width="100%"
      minHeight="1.5em"
      whiteSpace="nowrap"
      overflow="hidden"
      textOverflow="ellipsis"
      padding="5px 10px"
      bgcolor={background}
      onClick={onClick}
    >
      {children}
    </Box>
  );
};
