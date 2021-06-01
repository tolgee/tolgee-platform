import React, { ComponentProps, FunctionComponent } from 'react';
import { Box, Button } from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';

const LoadingButton: FunctionComponent<
  ComponentProps<typeof Button> & { loading?: boolean }
> = (props) => {
  const { disabled, loading, children, ...otherProps } = props;

  let isDisabled = props.loading || props.disabled;

  return (
    <Button disabled={isDisabled} {...otherProps}>
      {props.loading && (
        <Box display="flex" mr={1}>
          <CircularProgress size={20} />
        </Box>
      )}
      {children}
    </Button>
  );
};

export default LoadingButton;
