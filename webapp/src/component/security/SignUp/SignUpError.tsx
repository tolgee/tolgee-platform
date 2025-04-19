import React from 'react';
import { Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { LoadableType } from 'tg.component/common/form/StandardForm';
import { ResourceErrorComponent } from '../../common/form/ResourceErrorComponent';
import { Alert } from '../../common/Alert';
import { SpendingLimitExceededDescription } from './SpendingLimitExceededDesciption';

export const SignUpError: React.FC<{
  loadable: LoadableType;
}> = ({ loadable }) => {
  if (loadable.error?.code === 'seats_spending_limit_exceeded') {
    return (
      <Alert severity="error" data-cy="signup-error-seats-spending-limit">
        <Typography variant="h5" sx={{ mb: 1 }}>
          <T keyName="spending_limit_dialog_title" />
        </Typography>
        <SpendingLimitExceededDescription />
      </Alert>
    );
  }

  if (loadable.error?.code === 'free_self_hosted_seat_limit_exceeded') {
    return (
      <Alert severity="error" data-cy="signup-error-free-seat-limit">
        <Typography variant="h5" sx={{ mb: 1 }}>
          <T keyName="free_self_hosted_seat_limit_exceeded" />
        </Typography>
      </Alert>
    );
  }

  if (loadable.error?.code === 'plan_seat_limit_exceeded') {
    return (
      <Alert severity="error" data-cy="signup-error-plan-seat-limit">
        <Typography variant="h5" sx={{ mb: 1 }}>
          <T keyName="plan_seat_limit_exceeded" />
        </Typography>
      </Alert>
    );
  }

  return (
    <>
      {loadable && loadable.error && (
        <ResourceErrorComponent error={loadable.error} />
      )}
    </>
  );
};
