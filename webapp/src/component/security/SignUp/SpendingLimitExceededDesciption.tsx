import { T } from '@tolgee/react';
import { Link } from '@mui/material';

export const SpendingLimitExceededDescription = () => (
  <T
    keyName="spending_limit_dialog_description"
    params={{
      email: (
        <Link
          href="mailto:billing@tolgee.io"
          target="_blank"
          rel="noreferrer noopener"
        />
      ),
    }}
  />
);
