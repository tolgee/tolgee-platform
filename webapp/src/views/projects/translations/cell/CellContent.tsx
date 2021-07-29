import React, { RefObject } from 'react';
import { Box, BoxProps } from '@material-ui/core';

type CellContentProps = BoxProps & {
  forwardRef?: RefObject<HTMLElement | undefined | null>;
};

export const CellContent: React.FC<CellContentProps> = ({
  children,
  forwardRef,
  ...props
}) => {
  return (
    <Box margin="5px 10px" position="relative" flexGrow={1} {...props}>
      {children}
    </Box>
  );
};
