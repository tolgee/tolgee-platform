import { default as React, FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { ErrorResponseDto } from 'tg.service/response.types';

import { Alert } from '../Alert';

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
