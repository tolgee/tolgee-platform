import { Box, Button } from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';
import React, { ComponentProps, FunctionComponent } from 'react';

const LoadingButton: FunctionComponent<
  ComponentProps<typeof Button> & { loading?: boolean }
> = (props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disabled, loading, children, ...otherProps } = props;

  const isDisabled = props.loading || props.disabled;

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
