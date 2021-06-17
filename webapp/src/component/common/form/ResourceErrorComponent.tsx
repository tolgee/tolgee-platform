import { default as React, FunctionComponent } from 'react';
import { Alert } from '../Alert';
import { ErrorResponseDto } from '../../../service/response.types';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import { parseErrorResponse } from '../../../fixtures/errorFIxtures';

export const ResourceErrorComponent: FunctionComponent<{
  error: ErrorResponseDto | any;
}> = (props) => {
  return (
    <>
      {props.error &&
        parseErrorResponse(props.error).map((e, index) => (
          <Box ml={-2} mr={-2} key={index}>
            <Alert severity="error">
              <T>{e}</T>
            </Alert>
          </Box>
        ))}
    </>
  );
};
