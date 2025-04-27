import { default as React, FunctionComponent } from 'react';
import { Box } from '@mui/material';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { ErrorResponseDto } from 'tg.service/response.types';

import { Alert } from '../Alert';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

export const ResourceErrorComponent: FunctionComponent<{
  error: ErrorResponseDto | any;
  limit?: number;
}> = (props) => {
  return (
    <>
      {props.error &&
        parseErrorResponse(props.error)
          .slice(0, props.limit)
          .map((e, index) => (
            <Box key={index}>
              <Alert severity="error" data-cy="error-message">
                <TranslatedError code={e} />
              </Alert>
            </Box>
          ))}
    </>
  );
};
