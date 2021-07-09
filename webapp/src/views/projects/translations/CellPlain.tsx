import React from 'react';
import { Box, BoxProps } from '@material-ui/core';

type Props = {
  onClick?: React.MouseEventHandler<HTMLElement>;
  background?: string;
} & BoxProps;

export const CellPlain: React.FC<Props> = ({
  children,
  onClick,
  background,
  ...props
}) => {
  return (
    <Box
      width="100%"
      minHeight="1.5em"
      whiteSpace="nowrap"
      overflow="hidden"
      textOverflow="ellipsis"
      position="relative"
      padding="5px 10px"
      bgcolor={background}
      onClick={onClick}
      {...props}
    >
      {children}
    </Box>
  );
};
