import React, { ComponentProps, FunctionComponent } from 'react';
import { Box, Button } from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';

const LoadingButton: FunctionComponent<
  ComponentProps<typeof Button> & { loading?: boolean }
> = (props) => {
  const { disabled, loading, children, ...otherProps } = props;

  const isDisabled = loading || disabled;

  return (
    <Button disabled={isDisabled} {...otherProps}>
      {props.loading && (
        <Box
          display="flex"
          position="absolute"
          top="0"
          right="0"
          bottom="0"
          left="0"
          alignItems="center"
          justifyContent="center"
        >
          <CircularProgress size={20} />
        </Box>
      )}
      {children}
    </Button>
  );
};

export default LoadingButton;
